/*
 * A conta que um ADMIN cria, com a senha inicial dentro do corpo.
 *
 * O tamanho do e-mail (160) e do nome (120) batem com as colunas; a senha repete as 12 a
 * 72 caracteres da política de domínio, com a mesma mensagem que o validador daria —
 * recusa antecipada é o que evita gravar hash de senha fraca e descobrir depois.
 *
 * Role é o enum do domínio, aceito como nome (ADMIN ou OPERADOR): a apresentação não
 * mantém uma segunda lista de papéis para manter em dia.
 *
 * Não há flag de "conta ativa" aqui: toda conta criada por ADMIN nasce ativa, e nasce
 * também com troca de senha obrigatória — por isso o e-mail e a senha inicial podem ser
 * entregues juntos sem que ninguém opere com credencial que outra pessoa escolheu.
 */
package com.redekrill.scheduling.presentation.request;

import com.redekrill.scheduling.domain.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "Nome é obrigatório.")
        @Size(max = 120, message = "Nome deve ter no máximo 120 caracteres.")
        String name,

        @NotBlank(message = "E-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        @Size(max = 160, message = "E-mail deve ter no máximo 160 caracteres.")
        String email,

        @NotBlank(message = "Senha é obrigatória.")
        @Size(min = 12, max = 72, message = "Senha deve ter entre 12 e 72 caracteres.")
        String password,

        @NotNull(message = "Perfil é obrigatório.")
        Role role
) {
}
