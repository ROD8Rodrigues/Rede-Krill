/*
 * A conta de quem opera o sistema, seja para gerenciar ou para consultar.
 *
 * Este registro carrega o hash da senha, e isso é deliberado. O domínio não
 * conhece BCrypt — recebe o texto já cifrado por quem cria a conta e entrega o
 * mesmo texto para quem precisa conferir. O contrato de cifrar e conferir mora
 * em application.port.PasswordHasher e a execução em
 * infrastructure.security.BCryptPasswordHasherAdapter; o que existe aqui é o
 * dado, não o mecanismo.
 *
 * O hash nunca sai do servidor: UserResult leva id, nome, e-mail, papel,
 * atividade, a flag de troca e a data de criação — e é essa seleção que mantém a
 * senha dentro de casa, não nenhum código deste registro.
 *
 * mustChangePassword é a trava que o filtro de troca obrigatória consulta antes
 * de liberar qualquer outra rota. Ela acende em todo começo de conta — no seed,
 * quando o ADMIN cria um usuário e quando troca a senha de outro — e apaga no
 * momento em que o próprio usuário define uma senha nova.
 */
package com.redekrill.scheduling.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Usuário autenticável, com papel, situação de acesso e a pendência de troca de
 * senha.
 *
 * O que esta classe faz: exige nome, e-mail bem-formado, hash presente e papel
 * definido, e produz as variações usadas por cadastro, edição e troca de senha.
 *
 * O que ela NÃO faz: autenticar, cifrar nem comparar senha. Não há método
 * "verificaSenha" aqui de propósito — a comparação é responsabilidade de quem
 * conhece o algoritmo, e o domínio permanece independente dele.
 */
public record User(
        Long id,
        String name,
        String email,
        // Texto já cifrado. O domínio só transporta; nunca gera nem interpreta.
        String passwordHash,
        Role role,
        // false desliga a conta sem apagar o histórico dela.
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        // true obriga a troca no próximo acesso; o filtro de segurança lê este valor.
        boolean mustChangePassword,
        // Lock otimista do JPA; vem do banco, não é escrito pelas regras.
        long version
) {

    public User {
        name = DomainValidation.requireText(name, "Nome do usuário é obrigatório.");
        email = DomainValidation.requireEmail(email, "E-mail do usuário é obrigatório.");
        passwordHash = DomainValidation.requireText(passwordHash, "Senha do usuário é obrigatória.");
        Objects.requireNonNull(role, "Perfil do usuário é obrigatório.");
        Objects.requireNonNull(createdAt, "Data de criação é obrigatória.");
        Objects.requireNonNull(updatedAt, "Data de atualização é obrigatória.");
    }

    /**
     * Construtor de compatibilidade usado por fixtures e por código que não
     * precisa informar os campos de segurança introduzidos na etapa 2.
     */
    public User(Long id, String name, String email, String passwordHash, Role role,
                boolean active, Instant createdAt, Instant updatedAt) {
        this(id, name, email, passwordHash, role, active, createdAt, updatedAt, false, 0L);
    }

    /**
     * Fábrica sem pendência de troca, usada pelas fixtures de teste para montar
     * um usuário pronto em uma linha. A criação real de conta passa por
     * {@link #createInitial}, que decide a flag.
     *
     * @param name         nome exibido.
     * @param email        endereço de login; normalizado para minúscula.
     * @param passwordHash hash já cifrado por quem chama.
     * @param role         papel de acesso.
     * @param now          instante da criação, usado nas duas marcas de tempo.
     * @return o usuário sem id, ativo, sem pendência de senha.
     */
    public static User create(String name, String email, String passwordHash, Role role, Instant now) {
        return new User(null, name, email, passwordHash, role, true, now, now, false, 0L);
    }

    /**
     * Conta inicial — a do seed e a de quem recebe uma senha provisória. Aqui a
     * flag de troca vem de fora, porque quem definiu a senha pode não ser o dono
     * da conta.
     *
     * @param name              nome exibido.
     * @param email             endereço de login.
     * @param passwordHash      hash já cifrado.
     * @param role              papel de acesso.
     * @param mustChangePassword true quando a senha não foi escolhida pelo usuário.
     * @return o usuário sem id e ainda com versão zero.
     */
    public static User createInitial(String name, String email, String passwordHash, Role role,
                                     boolean mustChangePassword, Instant now) {
        return new User(null, name, email, passwordHash, role, true, now, now,
                mustChangePassword, 0L);
    }

    /**
     * Copia o valor com o id gerado pelo banco.
     *
     * @param newId identificador devolvido pela persistência.
     * @return igual, com id preenchido.
     */
    public User withId(Long newId) {
        return new User(newId, name, email, passwordHash, role, active, createdAt, updatedAt,
                mustChangePassword, version);
    }

    /**
     * Edição feita pelo ADMIN: dados de acesso, papel e situação.
     *
     * A pendência de senha não é tocada aqui — ela só muda de verdade em
     * {@link #resetPassword}, quando uma senha nova é de fato definida. Editar o
     * nome de alguém não deveria, por si só, obrigar aquela pessoa a trocar a
     * senha.
     *
     * @param newName        novo nome.
     * @param newEmail       novo e-mail; a unicidade é conferida no caso de uso.
     * @param newRole        novo papel.
     * @param newActive      false desliga o acesso sem apagar a conta.
     * @param newPasswordHash hash novo, ou o anterior quando ninguém mudou a senha.
     * @param now            instante da atualização.
     * @return o usuário editado, com a mesma criação e a mesma pendência.
     */
    public User update(String newName, String newEmail, Role newRole, boolean newActive,
                       String newPasswordHash, Instant now) {
        return new User(id, newName, newEmail, newPasswordHash, newRole, newActive, createdAt, now,
                mustChangePassword, version);
    }

    /**
     * Troca feita pelo próprio usuário, no fluxo que o filtro de troca obrigatória
     * permite. Apaga a pendência, porque a senha agora é escolha dele.
     *
     * @param newPasswordHash hash da senha nova.
     * @param now             instante da troca.
     * @return o usuário com a senha atualizada e a flag desligada.
     */
    public User changePassword(String newPasswordHash, Instant now) {
        return new User(id, name, email, newPasswordHash, role, active, createdAt, now,
                false, version);
    }

    /**
     * Senha definida por outra pessoa, no cadastro inicial ou pela edição do
     * ADMIN. Diferente de {@link #changePassword}: aqui a pendência é acendida,
     * porque quem vai usar a conta não foi quem escreveu aquela senha.
     *
     * @param newPasswordHash hash da senha provisória.
     * @param requireChange   se o próximo acesso deve obrigar a troca.
     * @param now             instante da definição.
     * @return o usuário com a nova senha e a pendência informada.
     */
    public User resetPassword(String newPasswordHash, boolean requireChange, Instant now) {
        return new User(id, name, email, newPasswordHash, role, active, createdAt, now,
                requireChange, version);
    }
}
