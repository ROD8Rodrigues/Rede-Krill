/*
 * O usuário sem a parte secreta.
 *
 * É a projeção usada em toda resposta que fala de pessoa: a listagem de ADMIN, o
 * /me do próprio usuário e o bloco dentro de SessionTokens na resposta de login. O
 * campo que não aparece aqui é o hash — dado que nem a apresentação nem o cliente
 * precisam ver, e uma vez exposto vale quanto uma senha.
 *
 * mustChangePassword é o campo que muda a experiência: com ele ligado, o filtro de
 * troca obrigatória segura quase todas as rotas até a pessoa definir a própria senha.
 */
package com.redekrill.scheduling.application.dto.user;

import com.redekrill.scheduling.domain.model.Role;
import com.redekrill.scheduling.domain.model.User;

import java.time.Instant;

/**
 * Projeção de leitura de User, sem credencial.
 */
public record UserResult(
        Long id,
        String name,
        String email,
        Role role,
        boolean active,
        boolean mustChangePassword,
        Instant createdAt
) {

    public static UserResult from(User user) {
        return new UserResult(
                user.id(),
                user.name(),
                user.email(),
                user.role(),
                user.active(),
                user.mustChangePassword(),
                user.createdAt()
        );
    }
}
