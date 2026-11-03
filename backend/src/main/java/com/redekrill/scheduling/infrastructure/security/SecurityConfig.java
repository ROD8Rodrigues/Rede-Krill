package com.redekrill.scheduling.infrastructure.security;

import com.redekrill.scheduling.application.dto.auth.AuthCookieSettings;
import com.redekrill.scheduling.infrastructure.config.properties.AuthProperties;
import com.redekrill.scheduling.infrastructure.config.properties.CorsProperties;
import com.redekrill.scheduling.infrastructure.config.properties.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public AuthCookieSettings authCookieSettings(AuthProperties authProperties,
                                                 SecurityProperties securityProperties) {
        return new AuthCookieSettings(
                authProperties.accessCookieName(),
                authProperties.refreshCookieName(),
                authProperties.cookieSecure(),
                authProperties.cookieSameSite(),
                authProperties.cookieDomain(),
                authProperties.exposeTokenInResponse(),
                securityProperties.trustForwardedHeaders()
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            PasswordChangeRequiredFilter passwordChangeRequiredFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler,
            CorsConfigurationSource corsConfigurationSource,
            AuthProperties authProperties,
            Environment environment
    ) throws Exception {
        boolean production = environment.matchesProfiles("prod");

        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookiePath("/");
        csrfRepository.setCookieCustomizer(cookie -> cookie
                .sameSite(authProperties.cookieSameSite())
                .secure(authProperties.cookieSecure()));

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        .ignoringRequestMatchers(new BearerAuthorizationRequestMatcher()))
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    authorize.requestMatchers(
                            "/api/auth/csrf",
                            "/api/auth/login",
                            "/api/auth/refresh",
                            "/api/auth/logout",
                            "/actuator/health",
                            "/actuator/health/**",
                            "/actuator/info",
                            "/error"
                    ).permitAll();
                    if (!production) {
                        authorize.requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll();
                    }
                    authorize.requestMatchers("/api/users/**", "/api/audit-logs/**").hasRole("ADMIN");
                    authorize.requestMatchers("/actuator/**").hasRole("ADMIN");
                    authorize.requestMatchers(HttpMethod.POST, "/api/truck-types/**").hasRole("ADMIN");
                    authorize.requestMatchers(HttpMethod.PUT, "/api/truck-types/**").hasRole("ADMIN");
                    authorize.requestMatchers(HttpMethod.DELETE, "/api/truck-types/**").hasRole("ADMIN");
                    authorize.requestMatchers(HttpMethod.POST, "/api/time-slots/**").hasRole("ADMIN");
                    authorize.requestMatchers(HttpMethod.PUT, "/api/time-slots/**").hasRole("ADMIN");
                    authorize.requestMatchers(HttpMethod.DELETE, "/api/time-slots/**").hasRole("ADMIN");
                    authorize.requestMatchers(HttpMethod.POST, "/api/palletization-types/**").hasRole("ADMIN");
                    authorize.requestMatchers(HttpMethod.PUT, "/api/palletization-types/**").hasRole("ADMIN");
                    authorize.requestMatchers(HttpMethod.DELETE, "/api/palletization-types/**").hasRole("ADMIN");
                    authorize.requestMatchers(HttpMethod.POST, "/api/appointments/**").hasRole("ADMIN");
                    authorize.requestMatchers(HttpMethod.PUT, "/api/appointments/**").hasRole("ADMIN");
                    authorize.requestMatchers(HttpMethod.DELETE, "/api/appointments/**").hasRole("ADMIN");
                    if (production) {
                        authorize.requestMatchers(
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
                        ).hasRole("ADMIN");
                    }
                    authorize.anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(passwordChangeRequiredFilter, JwtAuthenticationFilter.class)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With",
                "X-XSRF-TOKEN", "X-Correlation-ID"));
        configuration.setExposedHeaders(List.of("Location", "X-Correlation-ID"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
