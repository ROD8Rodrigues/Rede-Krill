/*
 * As duas credenciais do acesso, e nada ao redor.
 *
 * Não há "lembrar de mim", papel nem id de dispositivo: sessão, expiração e revogação são
 * decisão do serviço de autenticação, e aceitar esses campos aqui criaria a ilusão de que
 * o cliente manda no tamanho da sessão.
 *
 * O e-mail é validado com @Email antes de qualquer consulta — um endereço malformado não
 * tem como existir na tabela, e responder cedo economiza a leitura. Ele é comparado sem
 * diferenciar caixa, então a normalização acontece no caso de uso.
 *
 * O teto de 72 caracteres da senha está comentado junto do campo; a falha de login é a
 * mesma em conta inexistente e senha errada, de propósito, para não revelar quais
 * endereços têm conta.
 */
package com.redekrill.scheduling.presentation.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "E-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        @Size(max = 160, message = "E-mail deve ter no máximo 160 caracteres.")
        String email,

        // Limite espelha o da senha armazenada: BCrypt considera apenas os primeiros
        // 72 bytes, então corpo maior não autentica nada e só amplia o trabalho do servidor.
        @NotBlank(message = "Senha é obrigatória.")
        @Size(max = 72, message = "Senha deve ter no máximo 72 caracteres.")
        String password
) {
}
