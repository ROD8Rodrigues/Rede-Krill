/*
 * Contrato sobre as sessões de login.
 *
 * O detalhe que merece leitura é revokeIfActive: ele devolve booleano porque a
 * revogação é uma atualização condicionada — só mexe na linha que ainda está
 * ativa — e o "false" informa que alguém chegou antes. É assim que a rotação
 * detecta corrida entre duas requisições com o mesmo refresh token, sem depender
 * de lock pessimista nem de ler a sessão duas vezes.
 *
 * A busca é feita pelo hash do token e não por um identificador de sessão, porque
 * o que o cliente apresenta é o token. A rotação calcula o resumo e pergunta qual
 * linha corresponde; linha nenhuma significa token forjado ou já apagado.
 *
 * revokeAllByUserId derruba a família inteira de sessões de uma conta. É a
 * resposta para o reuso de token e para a troca de senha: nos dois casos, a dúvida
 * razoável recai sobre todas as sessões daquela pessoa, não só a que chegou.
 */
package com.redekrill.scheduling.domain.repository;

import com.redekrill.scheduling.domain.model.AuthSession;

import java.time.Instant;
import java.util.Optional;

/**
 * Persistência de sessões: criação, busca por hash de refresh, revogação pontual
 * ou em massa, e limpeza das vencidas.
 *
 * O que esta interface faz: descreve as operações que AuthSessionService executa
 * dentro de uma transação.
 *
 * O que ela NÃO faz: decidir se a sessão é válida. Essa conta é de
 * AuthSession.activeAt, que compara revogação e expiração com o relógio injetado.
 */
public interface AuthSessionRepository {

    AuthSession save(AuthSession session);

    Optional<AuthSession> findById(Long id);

    /**
     * Localiza a sessão a partir do resumo do token apresentado pelo cliente.
     */
    Optional<AuthSession> findByRefreshTokenHash(String refreshTokenHash);

    /**
     * Revoga a sessão se, e somente se, ela ainda estiver ativa.
     *
     * @param id  sessão a revogar.
     * @param now instante gravado como data de revogação.
     * @return true quando esta chamada foi a que virou a chave; false quando
     *         alguém revogou antes — sinal de corrida, tratado como reuso.
     */
    boolean revokeIfActive(Long id, Instant now);

    /**
     * Revogação em massa de todas as sessões vivas de uma conta.
     */
    void revokeAllByUserId(Long userId, Instant now);

    /**
     * Remove as sessões expiradas antes do instante informado e devolve quantas
     * linhas foram eliminadas.
     */
    int deleteExpiredBefore(Instant instant);
}
