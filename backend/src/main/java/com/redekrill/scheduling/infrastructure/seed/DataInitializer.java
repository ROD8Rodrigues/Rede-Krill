package com.redekrill.scheduling.infrastructure.seed;

import com.redekrill.scheduling.application.port.PasswordHasher;
import com.redekrill.scheduling.domain.model.Role;
import com.redekrill.scheduling.domain.model.User;
import com.redekrill.scheduling.domain.repository.UserRepository;
import com.redekrill.scheduling.infrastructure.config.properties.SeedProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final List<String> KNOWN_PASSWORDS = List.of(
            "admin@12345", "redekrill", "password", "changeme", "secret"
    );

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final SeedProperties seedProperties;
    private final Clock clock;
    private final Environment environment;

    public DataInitializer(UserRepository userRepository, PasswordHasher passwordHasher,
                           SeedProperties seedProperties, Clock clock, Environment environment) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.seedProperties = seedProperties;
        this.clock = clock;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!seedProperties.enabled()) {
            log.info("Seed de administrador desabilitado (SEED_ADMIN_ENABLED=false).");
            return;
        }

        String email = seedProperties.adminEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmail(email).isPresent()) {
            log.info("Usuário administrador inicial já existe: {}", email);
            return;
        }

        String password = seedProperties.adminPassword();
        if (password == null || password.isBlank()) {
            if (environment.matchesProfiles("prod")) {
                throw new IllegalStateException(
                        "SEED_ADMIN_PASSWORD é obrigatório quando o seed está habilitado em produção.");
            }
            log.warn("Seed de administrador ignorado: SEED_ADMIN_PASSWORD não configurado. "
                    + "Defina a variável para criar a conta inicial.");
            return;
        }
        if (password.length() < 12 || KNOWN_PASSWORDS.contains(password.toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException(
                    "SEED_ADMIN_PASSWORD deve ter pelo menos 12 caracteres e não pode ser "
                            + "uma senha padrão conhecida. Nenhuma senha foi registrada em log.");
        }

        User admin = User.createInitial(
                seedProperties.adminName(),
                email,
                passwordHasher.hash(password),
                Role.ADMIN,
                seedProperties.requirePasswordChange(),
                Instant.now(clock)
        );
        userRepository.save(admin);

        log.info("Usuário administrador inicial criado: {}. Troca de senha obrigatória: {}.",
                email, seedProperties.requirePasswordChange());
    }
}
