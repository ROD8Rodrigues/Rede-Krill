package com.redekrill.scheduling.domain.policy;

import com.redekrill.scheduling.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy();

    @Test
    void acceptsStrongPassword() {
        assertThatCode(() -> policy.validate("Operador@12345", "A senha"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsNullAndShortPasswords() {
        assertThatThrownBy(() -> policy.validate(null, "A senha"))
                .isInstanceOf(DomainException.class)
                .hasMessage("A senha deve ter pelo menos 12 caracteres.");
        assertThatThrownBy(() -> policy.validate("a".repeat(PasswordPolicy.MIN_LENGTH - 1), "A senha"))
                .isInstanceOf(DomainException.class)
                .hasMessage("A senha deve ter pelo menos 12 caracteres.");
    }

    @Test
    void rejectsPasswordsAboveTheEncoderLimit() {
        assertThatThrownBy(() -> policy.validate("a".repeat(PasswordPolicy.MAX_LENGTH) + "1", "A senha"))
                .isInstanceOf(DomainException.class)
                .hasMessage("A senha deve ter no máximo 72 caracteres.");
    }

    @Test
    void requiresBothLettersAndDigits() {
        assertThatThrownBy(() -> policy.validate("abcdefghijklmnop", "A senha"))
                .isInstanceOf(DomainException.class)
                .hasMessage("A senha deve conter letras e números.");
        assertThatThrownBy(() -> policy.validate("123456789012345", "A senha"))
                .isInstanceOf(DomainException.class)
                .hasMessage("A senha deve conter letras e números.");
    }

    @Test
    void rejectsKnownPasswordsRegardlessOfCase() {
        assertThatThrownBy(() -> policy.validate("admin@123456", "A nova senha"))
                .isInstanceOf(DomainException.class)
                .hasMessage("A nova senha é conhecida ou insegura. Escolha outra.");
        assertThatThrownBy(() -> policy.validate("ADMIN@123456", "A nova senha"))
                .isInstanceOf(DomainException.class)
                .hasMessage("A nova senha é conhecida ou insegura. Escolha outra.");
    }

    @Test
    void keepsTheShortEntriesOnlyForTheSeedCheck() {
        // "secret" tem 6 caracteres: validate() recusa por tamanho antes de olhar a lista,
        // mas o seed (que exige só 12+) ainda precisa reconhecê-la como senha conhecida.
        assertThat(policy.isKnownOrWeak("secret")).isTrue();
        assertThat(policy.isKnownOrWeak("SECRET")).isTrue();
        assertThat(policy.isKnownOrWeak(null)).isFalse();
        assertThat(policy.isKnownOrWeak("Operador@12345")).isFalse();
    }
}
