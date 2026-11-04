/*
 * A régua de senha do sistema inteiro.
 *
 * Um único lugar para conferir força de senha, porque a régua assimétrica deixava
 * uma porta aberta: o ADMIN conseguia cadastrar alguém com uma senha que o próprio
 * usuário, ao trocá-la, não poderia escolher. Toda criação, edição e troca passa
 * por aqui, e a comparação com a lista de senhas conhecidas usa a mesma base que o
 * seed consulta — separar as duas listas faria uma crescer enquanto a outra ainda
 * aceitava a senha já proibida.
 *
 * As faixas são explícitas por motivo técnico: 12 caracteres é o piso escolhido
 * para este sistema, e 72 é o limite que o BCrypt aceita como entrada, então a
 * recusa aqui acontece antes de o hash ser calculado. Além do tamanho, pedimos
 * letra e número — o mínimo que impede uma senha de dígitos puro.
 */
package com.redekrill.scheduling.domain.policy;

import com.redekrill.scheduling.domain.exception.DomainException;

import java.util.List;
import java.util.Locale;

/**
 * Regra única de senha do sistema, aplicada igual na criação, na atualização
 * pelo ADMIN e na troca feita pelo próprio usuário.
 *
 * <p>A política assimétrica anterior deixava o ADMIN cadastrar senha de 8 caracteres
 * sem composição — o caminho mais fraco para entrar uma credencial no sistema.</p>
 */
public final class PasswordPolicy {

    // Piso e teto de tamanho. O teto é o limite de entrada do BCrypt.
    public static final int MIN_LENGTH = 12;
    public static final int MAX_LENGTH = 72;

    private static final List<String> FORBIDDEN_PASSWORDS = List.of(
            "admin@12345", "redekrill", "password", "senha123456", "123456789012",
            "changeme", "secret",
            "admin@123456", "senha1234567", "redekrill123", "password1234", "administrador123"
    );

    /**
     * Devolve {@code true} quando a senha é uma credencial conhecida ou padrão de manual.
     * O seed de administrador usa a mesma base: com duas listas separadas, uma podia
     * crescer enquanto a outra ainda aceitava a senha já proibida pelo outro caminho.
     *
     * <p>As entradas curtas ou sem números ("redekrill", "secret", "123456789012") não
     * passariam pelas regras anteriores de {@link #validate} — ficam para o seed, que só
     * exige tamanho. As variantes de 12+ caracteres com letra e número é que tornam a
     * recusa por lista alcançável na criação e na troca de senha.</p>
     */
    public boolean isKnownOrWeak(String password) {
        return password != null && FORBIDDEN_PASSWORDS.contains(password.toLowerCase(Locale.ROOT));
    }

    /**
     * Confere a senha inteira e explica, na mensagem, o que faltou.
     *
     * A ordem das checagens é a de menor custo primeiro: tamanho, composição,
     * lista de conhecidas. As três primeiras são baratas; a comparação com a lista
     * só roda quando o texto já é plausível.
     *
     * @param password o texto cru, antes de qualquer hash.
     * @param label    como a mensagem se refere ao campo ("A senha", "A nova senha"),
     *                 para a mesma regra servir à criação e à troca.
     * @throws DomainException quando a senha não passa em alguma das checagens.
     */
    public void validate(String password, String label) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new DomainException(
                    "%s deve ter pelo menos %d caracteres.".formatted(label, MIN_LENGTH));
        }
        if (password.length() > MAX_LENGTH) {
            throw new DomainException(
                    "%s deve ter no máximo %d caracteres.".formatted(label, MAX_LENGTH));
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new DomainException("%s deve conter letras e números.".formatted(label));
        }
        if (isKnownOrWeak(password)) {
            throw new DomainException("%s é conhecida ou insegura. Escolha outra.".formatted(label));
        }
    }
}
