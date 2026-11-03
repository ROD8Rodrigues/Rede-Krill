package com.redekrill.scheduling.presentation.controller;

import com.redekrill.scheduling.application.dto.auth.AuthCookieSettings;
import com.redekrill.scheduling.application.dto.auth.AuthRequestAttributes;
import com.redekrill.scheduling.application.dto.auth.LoginCommand;
import com.redekrill.scheduling.application.dto.auth.SessionTokens;
import com.redekrill.scheduling.application.usecase.auth.ChangePasswordUseCase;
import com.redekrill.scheduling.application.usecase.auth.GetCurrentUserUseCase;
import com.redekrill.scheduling.application.usecase.auth.LoginUseCase;
import com.redekrill.scheduling.application.usecase.auth.LogoutUseCase;
import com.redekrill.scheduling.application.service.AuthSessionService;
import com.redekrill.scheduling.domain.exception.AuthenticationFailedException;
import com.redekrill.scheduling.presentation.mapper.WebMapper;
import com.redekrill.scheduling.presentation.request.ChangePasswordRequest;
import com.redekrill.scheduling.presentation.request.LoginRequest;
import com.redekrill.scheduling.presentation.response.LoginResponse;
import com.redekrill.scheduling.presentation.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação")
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final AuthSessionService authSessionService;
    private final AuthCookieSettings authSettings;

    public AuthController(LoginUseCase loginUseCase,
                          LogoutUseCase logoutUseCase,
                          GetCurrentUserUseCase getCurrentUserUseCase,
                          ChangePasswordUseCase changePasswordUseCase,
                          AuthSessionService authSessionService,
                          AuthCookieSettings authSettings) {
        this.loginUseCase = loginUseCase;
        this.logoutUseCase = logoutUseCase;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
        this.authSessionService = authSessionService;
        this.authSettings = authSettings;
    }

    @GetMapping("/csrf")
    @Operation(summary = "Obtém o token CSRF para requisições autenticadas por cookie")
    public Map<String, String> csrf(CsrfToken csrfToken) {
        return Map.of(
                "token", csrfToken.getToken(),
                "headerName", csrfToken.getHeaderName(),
                "parameterName", csrfToken.getParameterName()
        );
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica o usuário e cria uma sessão com cookies HttpOnly")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest,
                                               HttpServletResponse httpResponse) {
        SessionTokens tokens = loginUseCase.execute(new LoginCommand(
                request.email(),
                request.password(),
                clientIp(httpRequest),
                httpRequest.getHeader(HttpHeaders.USER_AGENT)
        ));
        setAuthCookies(httpResponse, tokens);
        String exposedToken = authSettings.exposeTokenInResponse() ? tokens.accessToken() : null;
        LoginResponse body = new LoginResponse(
                exposedToken,
                exposedToken == null ? null : "Bearer",
                tokens.accessExpiresIn(),
                WebMapper.toResponse(tokens.user())
        );
        return ResponseEntity.ok(body);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renova a sessão usando o refresh token rotativo")
    public ResponseEntity<LoginResponse> refresh(HttpServletRequest httpRequest,
                                                 HttpServletResponse httpResponse) {
        String refreshToken = readCookie(httpRequest, authSettings.refreshCookieName());
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthenticationFailedException("Sessão não informada. Autentique-se novamente.");
        }
        SessionTokens tokens = authSessionService.rotate(
                refreshToken, clientIp(httpRequest), httpRequest.getHeader(HttpHeaders.USER_AGENT));
        setAuthCookies(httpResponse, tokens);
        String exposedToken = authSettings.exposeTokenInResponse() ? tokens.accessToken() : null;
        return ResponseEntity.ok(new LoginResponse(
                exposedToken,
                exposedToken == null ? null : "Bearer",
                tokens.accessExpiresIn(),
                WebMapper.toResponse(tokens.user())
        ));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "cookieAuth")
    @Operation(summary = "Revoga a sessão atual e remove os cookies")
    public void logout(HttpServletRequest request, HttpServletResponse response,
                       Authentication authentication) {
        String refreshToken = readCookie(request, authSettings.refreshCookieName());
        authSessionService.revokeRefreshToken(refreshToken);
        Object sessionId = request.getAttribute(AuthRequestAttributes.SESSION_ID);
        if (sessionId instanceof Long id) {
            authSessionService.revokeSession(id);
        }
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                logoutUseCase.execute(authentication.getName());
            } catch (AuthenticationFailedException ignored) {
                // A sessão já pode ter sido revogada; os cookies serão removidos de qualquer forma.
            }
        }
        clearAuthCookies(response);
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "cookieAuth")
    @Operation(summary = "Troca a senha do usuário autenticado e encerra todas as sessões")
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request,
                               Authentication authentication,
                               HttpServletResponse response) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationFailedException("Autenticação necessária.");
        }
        changePasswordUseCase.execute(
                authentication.getName(), request.currentPassword(), request.newPassword());
        clearAuthCookies(response);
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "cookieAuth")
    @Operation(summary = "Retorna o usuário autenticado")
    public UserResponse me(Authentication authentication) {
        return WebMapper.toResponse(getCurrentUserUseCase.execute(authentication.getName()));
    }

    private void setAuthCookies(HttpServletResponse response, SessionTokens tokens) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(
                authSettings.accessCookieName(), tokens.accessToken(),
                Duration.ofSeconds(tokens.accessExpiresIn()), "/").toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(
                authSettings.refreshCookieName(), tokens.refreshToken(),
                Duration.ofSeconds(tokens.refreshExpiresIn()), "/api/auth").toString());
    }

    private void clearAuthCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(
                authSettings.accessCookieName(), "", Duration.ZERO, "/").toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(
                authSettings.refreshCookieName(), "", Duration.ZERO, "/api/auth").toString());
    }

    private ResponseCookie cookie(String name, String value, Duration maxAge, String path) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(authSettings.cookieSecure())
                .sameSite(authSettings.cookieSameSite())
                .path(path)
                .maxAge(maxAge);
        if (authSettings.cookieDomain() != null && !authSettings.cookieDomain().isBlank()) {
            builder.domain(authSettings.cookieDomain());
        }
        return builder.build();
    }

    private String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        if (authSettings.trustForwardedHeaders()) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
