package com.redekrill.scheduling.infrastructure.security;

import com.redekrill.scheduling.application.port.TokenIssuer;
import com.redekrill.scheduling.application.port.IssuedToken;
import com.redekrill.scheduling.domain.model.Role;
import com.redekrill.scheduling.domain.model.User;
import com.redekrill.scheduling.infrastructure.config.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtTokenService implements TokenIssuer {

    private final SecretKey key;
    private final Duration expiration;
    private final Clock clock;

    public JwtTokenService(JwtProperties properties, Clock clock) {
        this.key = createKey(properties.secret());
        this.expiration = properties.expiration();
        this.clock = clock;

        if (expiration.isZero() || expiration.isNegative()) {
            throw new IllegalStateException("JWT_EXPIRATION deve ser maior que zero.");
        }
    }

    @Override
    public IssuedToken issue(User user) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(expiration);

        String token = Jwts.builder()
                .subject(user.email())
                .claim("role", user.role().name())
                .claim("name", user.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();

        return new IssuedToken(token, expiration.toSeconds());
    }

    public Optional<JwtPrincipal> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String email = claims.getSubject();
            String role = claims.get("role", String.class);
            if (email == null || role == null) {
                return Optional.empty();
            }
            return Optional.of(new JwtPrincipal(email, Role.valueOf(role)));
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private SecretKey createKey(String secret) {
        try {
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        } catch (IllegalArgumentException | io.jsonwebtoken.security.WeakKeyException exception) {
            throw new IllegalStateException(
                    "JWT_SECRET inválido. Informe um segredo Base64 com pelo menos 256 bits.", exception);
        }
    }

    public record JwtPrincipal(String email, Role role) {
    }
}
