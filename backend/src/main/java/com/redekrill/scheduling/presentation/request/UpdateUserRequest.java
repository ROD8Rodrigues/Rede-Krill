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

        boolean active,

        @Size(min = 8, max = 72, message = "Senha deve ter entre 8 e 72 caracteres.")
        String password
) {
}
