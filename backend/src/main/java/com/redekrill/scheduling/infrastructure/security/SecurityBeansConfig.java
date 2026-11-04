/*
 * Onde nasce o PasswordEncoder usado para gravar e conferir senha.
 *
 * O custo do BCrypt não está escrito no código: vem de app.security.bcrypt-strength,
 * validado entre 10 e 20 em SecurityProperties. Aumentar esse valor deixa login e
 * criação de usuário mais lentos de propósito — é a conta cobrada de quem tenta
 * adivinhar senha em bateria.
 */
package com.redekrill.scheduling.infrastructure.security;

import com.redekrill.scheduling.infrastructure.config.properties.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Expõe o bean de codificação de senha. Separado de SecurityConfig para que a cadeia de
 * filtros não virasse o único lugar onde objetos de segurança são produzidos.
 */
@Configuration
public class SecurityBeansConfig {

    @Bean
    public PasswordEncoder passwordEncoder(SecurityProperties securityProperties) {
        return new BCryptPasswordEncoder(securityProperties.bcryptStrength());
    }
}
