/*
 * Edição de conta: nome, e-mail, papel e situação obrigatórios, senha opcional.
 *
 * active vem como Boolean, não boolean, porque a anotação @NotNull precisa poder
 * distinguir "mandou false" de "não mandou". A lista completa é exigida no PUT por um
 * motivo prático: esquecer de mandar o papel não pode significar rebaixar ninguém para o
 * padrão.
 *
 * password é o único campo que pode faltar, e o significado de faltar é "mantém a atual".
 * Quando vem, o caso de uso exige os mesmos 12 a 72 caracteres, troca o hash, marca troca
 * obrigatória e derruba as sessões da conta — é ADMIN alterando a credencial de outra
 * pessoa, não a troca própria, que tem rota em /api/auth/change-password.
 */
package com.redekrill.scheduling.presentation.request;

import com.redekrill.scheduling.domain.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank(message = "Nome é obrigatório.")
        @Size(max = 120, message = "Nome deve ter no máximo 120 caracteres.")
        String name,

        @NotBlank(message = "E-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        @Size(max = 160, message = "E-mail deve ter no máximo 160 caracteres.")
        String email,

        @NotNull(message = "Perfil é obrigatório.")
        Role role,

        @NotNull(message = "Situação (active) é obrigatória.")
        Boolean active,

        @Size(min = 12, max = 72, message = "Senha deve ter entre 12 e 72 caracteres.")
        String password
) {
}
