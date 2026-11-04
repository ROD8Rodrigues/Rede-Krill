package com.redekrill.scheduling.infrastructure.security;

import com.redekrill.scheduling.application.port.IssuedToken;
import com.redekrill.scheduling.domain.model.Role;
import com.redekrill.scheduling.domain.model.User;
import com.redekrill.scheduling.infrastructure.config.properties.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Expiração e assinatura são o contrato de segurança do access token. O serviço recebe um
 * {@link Clock} e o usa também na verificação, então estes casos rodam em tempo determinístico
 * em vez de esperar 15 minutos de relógio real.
 */
class JwtTokenServiceTest {

    private static final Instant START = Instant.parse("2026-02-01T12:00:00Z");
    private static final Duration EXPIRATION = Duration.ofMinutes(15);
    private static final String SECRET = base64("chave-de-teste-com-mais-de-256-bits-de-entropia-0123456789");
    private static final User OPERADOR =
            User.create("Operador", "operador@redekrill.com", "hash", Role.USER, START);

    private final MutableClock clock = new MutableClock(START);
    private final JwtTokenService service = service(SECRET, EXPIRATION);

    @Test
    void issuesAndParsesThePrincipalWithTheSessionClaim() {
        IssuedToken issued = service.issue(OPERADOR, 42L);

        assertThat(issued.expiresInSeconds()).isEqualTo(EXPIRATION.toSeconds());
        JwtTokenService.JwtPrincipal principal = orFail(service.parse(issued.value()));
        assertThat(principal.email()).isEqualTo("operador@redekrill.com");
        assertThat(principal.role()).isEqualTo(Role.USER);
        assertThat(principal.sessionId()).isEqualTo(42L);
    }

    @Test
    void parsesTokensWithoutSessionClaimAsNullSession() {
        assertThat(orFail(service.parse(service.issue(OPERADOR, null).value())).sessionId()).isNull();
    }

    @Test
    void rejectsTheTokenOnceTheExpirationHasPassed() {
        IssuedToken issued = service.issue(OPERADOR, 42L);

        clock.advanceSeconds(EXPIRATION.toSeconds() - 1);
        assertThat(service.parse(issued.value())).isPresent();

        clock.advance(Duration.ofSeconds(2));
        assertThat(service.parse(issued.value()))
                .as("access token vencido não pode autenticar pedido")
                .isEmpty();
    }

    @Test
    void rejectsATokenSignedWithAnotherSecret() {
        IssuedToken foreign = service(base64("outra-chave-tambem-longissima-para-passar-em-256-bits-9876543210"),
                EXPIRATION).issue(OPERADOR, 42L);

        assertThat(service.parse(foreign.value())).isEmpty();
    }

    @Test
    void rejectsGarbageWithoutThrowing() {
        assertThat(service.parse("isso-nao-e-um-jwt")).isEmpty();
        assertThat(service.parse("")).isEmpty();
    }

    @Test
    void rejectsASignatureWithoutRoleClaim() {
        assertThat(service.parse(signedWithClaims("operador@redekrill.com", null)))
                .as("papel ausente não pode virar autenticação com privilégio padrão")
                .isEmpty();
    }

    @Test
    void rejectsAnUnknownRoleInsideTheToken() {
        assertThat(service.parse(signedWithClaims("operador@redekrill.com", "SUPERUSER"))).isEmpty();
    }

    @Test
    void refusesToStartWithoutSecretOutsideDevAndTest() {
        MockEnvironment ambiente = new MockEnvironment();
        ambiente.setActiveProfiles("prod");

        assertThatThrownBy(() -> new JwtTokenService(new JwtProperties("", EXPIRATION), clock, ambiente))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET é obrigatório");
    }

    @Test
    void usesEphemeralSecretInDevAndKeepsWorkingInsideTheSameInstance() {
        JwtTokenService servico = new JwtTokenService(new JwtProperties(null, EXPIRATION), clock, dev());
        IssuedToken issued = servico.issue(OPERADOR, 42L);

        assertThat(servico.parse(issued.value())).isPresent();
    }

    @Test
    void reportsAnActionableErrorForAMalformedBase64Secret() {
        assertThatThrownBy(() -> service("@@segredo-que-nao-e-base64@@", EXPIRATION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET inválido")
                .hasRootCauseInstanceOf(io.jsonwebtoken.io.DecodingException.class);
    }

    @Test
    void reportsAnActionableErrorForAShortSecret() {
        assertThatThrownBy(() -> service(base64("curta"), EXPIRATION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET inválido")
                .hasRootCauseInstanceOf(io.jsonwebtoken.security.WeakKeyException.class);
    }

    @Test
    void rejectsNonPositiveExpiration() {
        assertThatThrownBy(() -> new JwtTokenService(new JwtProperties(SECRET, Duration.ZERO), clock, dev()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_EXPIRATION");

        assertThatCode(() -> service(SECRET, EXPIRATION)).doesNotThrowAnyException();
    }

    private JwtTokenService service(String secret, Duration expiration) {
        return new JwtTokenService(new JwtProperties(secret, expiration), clock, dev());
    }

    private MockEnvironment dev() {
        MockEnvironment ambiente = new MockEnvironment();
        ambiente.setActiveProfiles("dev");
        return ambiente;
    }

    private String signedWithClaims(String subject, String role) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        var builder = Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(START))
                .expiration(Date.from(START.plus(EXPIRATION)));
        if (role != null) {
            builder.claim("role", role);
        }
        return builder.signWith(key).compact();
    }

    private static JwtTokenService.JwtPrincipal orFail(Optional<JwtTokenService.JwtPrincipal> principal) {
        return principal.orElseThrow(() -> new AssertionError("token válido não deveria ser rejeitado"));
    }

    private static String base64(String raw) {
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        private void advance(Duration duration) {
            now = now.plus(duration);
        }

        private void advanceSeconds(long seconds) {
            now = now.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
