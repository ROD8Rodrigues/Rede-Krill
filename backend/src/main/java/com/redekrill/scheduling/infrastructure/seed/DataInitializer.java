/*
 * A primeira conta ADMIN do banco, criada no boot quando o seed está ligado.
 *
 * Três caminhos terminam antes de qualquer escrita, e cada um é idempotente de um jeito
 * diferente: seed desligado sai quieto; e-mail já existente sai sem duplicar — por isso
 * a consulta usa o endereço já em minúsculas, igual ao índice único da tabela; e senha
 * ausente vira erro em produção e aviso nos outros perfis, porque desenvolvedor sem
 * variável setada precisa de um log, não de um boot quebrado.
 *
 * A senha é conferida pelo PasswordPolicy antes de qualquer hash: mínimo de 12 caracteres
 * e fora da lista de valores conhecidos. Passar por cima disso criaria exatamente a
 * conta que a política existe para impedir, com a diferença de que aqui ninguém viu a
 * política sendo violada.
 *
 * A senha em texto nunca vai para o log — a mensagem de erro diz o que corrigir e deixa
 * o valor de fora. O que é gravado é só o hash, junto da flag de troca obrigatória que
 * SeedProperties define.
 */
package com.redekrill.scheduling.infrastructure.seed;

import com.redekrill.scheduling.application.port.PasswordHasher;
import com.redekrill.scheduling.domain.model.Role;
import com.redekrill.scheduling.domain.model.User;
import com.redekrill.scheduling.domain.policy.PasswordPolicy;
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
import java.util.Locale;

/**
 * Cria o administrador inicial via ApplicationRunner. Não popula dados de domínio (tipos
 * de caminhão, horários) nem altera usuário existente: se a conta já está lá, para aqui.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final SeedProperties seedProperties;
    private final Clock clock;
    private final Environment environment;
    private final PasswordPolicy passwordPolicy;

    public DataInitializer(UserRepository userRepository, PasswordHasher passwordHasher,
                           SeedProperties seedProperties, Clock clock, Environment environment,
                           PasswordPolicy passwordPolicy) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.seedProperties = seedProperties;
        this.clock = clock;
        this.environment = environment;
        this.passwordPolicy = passwordPolicy;
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
        if (password.length() < PasswordPolicy.MIN_LENGTH || passwordPolicy.isKnownOrWeak(password)) {
            throw new IllegalStateException(
                    "SEED_ADMIN_PASSWORD deve ter pelo menos %d caracteres e não pode ser "
                            + "uma senha padrão conhecida. Nenhuma senha foi registrada em log."
                            .formatted(PasswordPolicy.MIN_LENGTH));
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
