/*
 * Tradução mínima entre o port PasswordHasher e o PasswordEncoder do Spring.
 *
 * O corpo tem duas chamadas, e é assim de propósito: quem escreve o caso de uso não
 * precisa saber que existe BCrypt atrás do port, e trocar o algoritmo um dia é mexer
 * apenas aqui e no bean que produz o codificador.
 *
 * matches recebe a senha em texto e o hash guardado — nunca dois hashes. O BCrypt guarda
 * o sal dentro do próprio resultado, então comparar strings já cifradas entre si não
 * significaria nada.
 */
package com.redekrill.scheduling.infrastructure.security;

import com.redekrill.scheduling.application.port.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Adapter de PasswordHasher. Não aplica política de senha: tamanho e complexidade são
 * validados na entrada, antes de qualquer hash acontecer.
 */
@Component
public class BCryptPasswordHasherAdapter implements PasswordHasher {

    private final PasswordEncoder passwordEncoder;

    public BCryptPasswordHasherAdapter(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return passwordEncoder.matches(rawPassword, passwordHash);
    }
}
