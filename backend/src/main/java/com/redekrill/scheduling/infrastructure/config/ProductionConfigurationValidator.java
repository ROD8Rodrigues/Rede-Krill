package com.redekrill.scheduling.infrastructure.config;

import com.redekrill.scheduling.infrastructure.config.properties.AuthProperties;
import com.redekrill.scheduling.infrastructure.config.properties.JwtProperties;
import com.redekrill.scheduling.infrastructure.config.properties.SeedProperties;
import io.jsonwebtoken.io.Decoders;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.util.List;
import java.util.Locale;

/**
 * Falha rapidamente quando o perfil de produção é iniciado com configuração
 * insegura. Não registra os valores analisados.
 */
@Configuration
@Profile("prod")
public class ProductionConfigurationValidator {

    private static final List<String> KNOWN_SECRETS = List.of(
            "admin@12345", "redekrill", "password", "changeme", "secret"
    );

    private final Environment environment;
    private final JwtProperties jwtProperties;
    private final AuthProperties authProperties;
    private final SeedProperties seedProperties;

    public ProductionConfigurationValidator(Environment environment,
                                            JwtProperties jwtProperties,
                                            AuthProperties authProperties,
                                            SeedProperties seedProperties) {
        this.environment = environment;
        this.jwtProperties = jwtProperties;
        this.authProperties = authProperties;
        this.seedProperties = seedProperties;
    }

    @PostConstruct
    void validate() {
        String dbPassword = environment.getProperty("spring.datasource.password", "");
        if (dbPassword.isBlank() || KNOWN_SECRETS.contains(dbPassword.toLowerCase(Locale.ROOT))) {
            fail("DB_PASSWORD obrigatório e não pode ser um valor padrão conhecido.");
        }

        String jwtSecret = jwtProperties.secret();
        if (jwtSecret == null || jwtSecret.isBlank()) {
            fail("JWT_SECRET é obrigatório em produção.");
        }
        try {
            if (Decoders.BASE64.decode(jwtSecret).length < 32) {
                fail("JWT_SECRET deve ter pelo menos 256 bits (32 bytes) em Base64.");
            }
        } catch (RuntimeException exception) {
            fail("JWT_SECRET deve ser um valor Base64 válido.");
        }

        if (!authProperties.cookieSecure()) {
            fail("AUTH_COOKIE_SECURE deve ser true em produção.");
        }
        if (authProperties.exposeTokenInResponse()) {
            fail("AUTH_EXPOSE_TOKEN_IN_RESPONSE deve ser false em produção.");
        }
        if (seedProperties.enabled()) {
            String seedPassword = seedProperties.adminPassword();
            if (seedPassword == null || seedPassword.isBlank()
                    || seedPassword.length() < 12
                    || KNOWN_SECRETS.contains(seedPassword.toLowerCase(Locale.ROOT))) {
                fail("SEED_ADMIN_PASSWORD deve ser forte (mínimo 12 caracteres) quando o seed "
                        + "estiver habilitado em produção. Prefira desabilitar o seed após o "
                        + "primeiro provisionamento (SEED_ADMIN_ENABLED=false).");
            }
        }
    }

    private void fail(String message) {
        throw new IllegalStateException("Configuração de produção insegura: " + message);
    }
}
