package com.redekrill.scheduling.infrastructure.security;

import com.redekrill.scheduling.application.dto.auth.AuthRequestAttributes;
import com.redekrill.scheduling.application.service.AuthSessionService;
import com.redekrill.scheduling.domain.model.User;
import com.redekrill.scheduling.domain.repository.UserRepository;
import com.redekrill.scheduling.infrastructure.config.properties.AuthProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;
    private final UserRepository userRepository;
    private final AuthSessionService authSessionService;
    private final AuthProperties authProperties;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService,
                                   UserRepository userRepository,
                                   AuthSessionService authSessionService,
                                   AuthProperties authProperties) {
        this.jwtTokenService = jwtTokenService;
        this.userRepository = userRepository;
        this.authSessionService = authSessionService;
        this.authProperties = authProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = resolveToken(request);
            if (token != null) {
                authenticate(request, token);
            }
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String token) {
        jwtTokenService.parse(token)
                .filter(principal -> principal.sessionId() != null)
                .filter(principal -> authSessionService.isActive(principal.sessionId()))
                .ifPresent(principal -> userRepository.findByEmail(principal.email())
                        .filter(User::active)
                        .ifPresent(user -> {
                            request.setAttribute(AuthRequestAttributes.MUST_CHANGE_PASSWORD,
                                    user.mustChangePassword());
                            request.setAttribute(AuthRequestAttributes.SESSION_ID, principal.sessionId());

                            List<SimpleGrantedAuthority> authorities =
                                    List.of(new SimpleGrantedAuthority(
                                            "ROLE_" + user.role().name()));
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            user.email(), null, authorities);
                            authentication.setDetails(
                                    new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        }));
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            return token.isEmpty() ? null : token;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (authProperties.accessCookieName().equals(cookie.getName())) {
                return cookie.getValue() == null || cookie.getValue().isBlank()
                        ? null : cookie.getValue();
            }
        }
        return null;
    }
}
