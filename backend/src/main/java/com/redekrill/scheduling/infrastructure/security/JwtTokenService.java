/*
 * Assinatura e leitura dos access tokens.
 *
 * O contrato com a aplicação é o port TokenIssuer; aqui vive somente o jeito JWT de
 * cumpri-lo: claims, chave e expiração. Quem decide se a sessão ainda vale não é esta
 * classe — o filtro joga essa dúvida para AuthSessionService.
 *
 * Um detalhe que economiza dor de cabeça: a chave é lida como Base64. Configurar o
 * segredo em texto puro faz o decode gerar bytes diferentes dos que você imagina, e o
 * token passa a ser assinado com uma chave que ninguém escolheu de verdade.
 *
 * Em dev e test, a ausência de JWT_SECRET produz um segredo aleatório a cada boot. É
 * conveniente e desconcertante ao mesmo tempo: os tokens emitidos antes de reiniciar
 * param de validar sozinhos, e isso não é bug de parse, é a chave nova. Em produção a
 * mesma ausência derruba a aplicação, de propósito.
 */
package com.redekrill.scheduling.infrastructure.security;

import com.redekrill.scheduling.application.port.IssuedToken;
import com.redekrill.scheduling.application.port.TokenIssuer;
import com.redekrill.scheduling.domain.model.Role;
import com.redekrill.scheduling.domain.model.User;
import com.redekrill.scheduling.infrastructure.config.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

/**
 * Emite e interpreta JWT assinado com HMAC, usando o mesmo Clock na emissão e na validação.
 * Não consulta sessões nem decide acesso — só responde se o token é autêntico e o que ele diz.
 */
@Component
public class JwtTokenService implements TokenIssuer {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenService.class);

    private final SecretKey key;
    private final Duration expiration;
    private final Clock clock;

    public JwtTokenService(JwtProperties properties, Clock clock, Environment environment) {
        this.key = createKey(properties.secret(), environment);
        this.expiration = properties.expiration();
        this.clock = clock;

        if (expiration.isZero() || expiration.isNegative()) {
            throw new IllegalStateException("JWT_EXPIRATION deve ser maior que zero.");
        }
    }

    @Override
    public IssuedToken issue(User user, Long sessionId) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(expiration);

        var builder = Jwts.builder()
                .subject(user.email())
                .claim("role", user.role().name())
                .claim("name", user.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt));
        if (sessionId != null) {
            builder.claim("sid", sessionId);
        }
        String token = builder.signWith(key).compact();

        return new IssuedToken(token, expiration.toSeconds());
    }

    public Optional<JwtPrincipal> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    // mesma fonte de tempo da emissão: mantém a expiração determinística
                    // em teste e evita relógios diferentes nos dois lados do contrato.
                    .clock(() -> Date.from(Instant.now(clock)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String email = claims.getSubject();
            String role = claims.get("role", String.class);
            if (email == null || role == null) {
                return Optional.empty();
            }
            Object sid = claims.get("sid");
            Long sessionId = sid instanceof Number number ? number.longValue() : null;
            return Optional.of(new JwtPrincipal(email, Role.valueOf(role), sessionId));
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private SecretKey createKey(String secret, Environment environment) {
        if (secret == null || secret.isBlank()) {
            if (environment.matchesProfiles("dev", "test")) {
                byte[] generated = new byte[32];
                new SecureRandom().nextBytes(generated);
                log.warn("JWT_SECRET não configurado. Usando segredo efêmero de desenvolvimento; "
                        + "tokens serão invalidados ao reiniciar a aplicação.");
                return Keys.hmacShaKeyFor(generated);
            }
            throw new IllegalStateException(
                    "JWT_SECRET é obrigatório. Informe um segredo Base64 com pelo menos 256 bits.");
        }
        try {
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        } catch (IllegalArgumentException | JwtException exception) {
            // JwtException cobre DecodingException (Base64 malformado) e WeakKeyException
            // (segredo curto): sem capturá-los, o boot morria com "Illegal base64 character"
            // em vez desta mensagem acionável.
            throw new IllegalStateException(
                    "JWT_SECRET inválido. Informe um segredo Base64 com pelo menos 256 bits.", exception);
        }
    }

    public record JwtPrincipal(String email, Role role, Long sessionId) {
    }
}
