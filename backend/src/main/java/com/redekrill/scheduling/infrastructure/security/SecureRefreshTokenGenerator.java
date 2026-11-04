/*
 * Produção e hash do refresh token.
 *
 * O refresh é opaco de caso pensado: 32 bytes de SecureRandom em Base64 URL sem padding,
 * sem claims e sem assinatura. Ele não carrega informação alguma, então quem intercepta
 * não lê nada — e a revogação é imediata, porque a decisão passa a ser uma linha no
 * banco, não o vencimento de um JWT.
 *
 * No banco vai o hash SHA-256 em hexadecimal, nunca o valor que o cliente recebeu.
 * Vazamento de tabela, portanto, não entrega token utilizável. O hash é determinístico e
 * comparável, que é exatamente o que a rotação precisa.
 *
 * hash() recusa texto em branco: receber nulo ou vazio é erro de quem chamou, não
 * condição de negócio, e deixar passar geraria hash de algo que não existe.
 */
package com.redekrill.scheduling.infrastructure.security;

import com.redekrill.scheduling.application.port.RefreshTokenGenerator;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Implementa RefreshTokenGenerator com aleatoriedade criptográfica e hash de comparação.
 * Não persiste nem revoga sessão — isso fica com AuthSessionService e o repositório.
 */
@Component
public class SecureRefreshTokenGenerator implements RefreshTokenGenerator {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String hash(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Token de refresh é obrigatório.");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível.", exception);
        }
    }
}
