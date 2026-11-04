/*
 * Os dois textos necessários para trocar a senha de quem já está logado.
 *
 * currentPassword é obrigatória porque a troca não é uma edição de perfil: sem reescrever
 * a senha antiga, qualquer request forjada a partir de uma sessão aberta assumiria a
 * conta. O campo não tem tamanho máximo — só comparação com o hash.
 *
 * newPassword repete o intervalo de 12 a 72 caracteres da criação de usuário, de
 * propósito: se as duas rotas divergissem, a conta criaria com uma senha que ela mesma
 * não consegue trocar depois. O teto de 72 é o limite do BCrypt, não da política.
 */
package com.redekrill.scheduling.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Senha atual é obrigatória.")
        String currentPassword,

        @NotBlank(message = "Nova senha é obrigatória.")
        @Size(min = 12, max = 72, message = "Nova senha deve ter entre 12 e 72 caracteres.")
        String newPassword
) {
}
