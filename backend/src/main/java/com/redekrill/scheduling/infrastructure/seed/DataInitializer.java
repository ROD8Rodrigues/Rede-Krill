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
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String DEFAULT_PASSWORD = "Admin@12345";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final SeedProperties seedProperties;
    private final Clock clock;

    public DataInitializer(UserRepository userRepository, PasswordHasher passwordHasher,
                           SeedProperties seedProperties, Clock clock) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.seedProperties = seedProperties;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        String email = seedProperties.adminEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.findByEmail(email).isPresent()) {
            log.info("Usuário administrador inicial já existe: {}", email);
            return;
        }

        User admin = User.create(
                seedProperties.adminName(),
                email,
                passwordHasher.hash(seedProperties.adminPassword()),
                Role.ADMIN,
                Instant.now(clock)
        );
        userRepository.save(admin);

        log.info("Usuário administrador inicial criado: {}", email);
        if (DEFAULT_PASSWORD.equals(seedProperties.adminPassword())) {
            log.warn("A senha padrão do administrador está em uso. Altere SEED_ADMIN_PASSWORD em produção.");
        }
    }
}
