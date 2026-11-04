/*
 * A linha que conta o que aconteceu, para quem consulta depois.
 *
 * Auditoria aqui é tabela, não arquivo de texto: a listagem é consultável pela
 * rota que só o ADMIN abre, e cada linha diz quem fez, o que fez, em que registro
 * e quando. Nas ações que alteram dados, o caso de uso chama AuditService dentro
 * da mesma transação: se o insert do agendamento falhar, não sobra um log contando
 * um agendamento que não existe.
 *
 * actorEmail guarda o e-mail, e não uma referência a User. É escolha consciente:
 * se a conta for desligada ou renomeada, a linha antiga continua dizendo quem
 * agiu. Uma foreign key obrigatória tornaria o histórico dependente do cadastro.
 *
 * entityName e entityId são texto livre, sem join com nada. É o preço de um log
 * que cobre várias tabelas com a mesma estrutura, e o motivo pelo qual entityId é
 * String: o serviço de auditoria recebe o id numérico, converte, e aceita null
 * quando a ação não aponta para um registro único — como logout e negação de
 * acesso.
 */
package com.redekrill.scheduling.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Registro imutável de uma ação: autor, tipo de ação, entidade afetada e o
 * contexto em texto.
 *
 * O que esta classe faz: exige autor com e-mail válido, ação e nome de entidade,
 * e normaliza os campos opcionais.
 *
 * O que ela NÃO faz: mudar de ideia. Não existe método de edição — linha de
 * auditoria que se reescreve deixa de ser prova. Quem precisa corrigir um fato
 * registra outra linha, não ajusta a anterior.
 */
public record AuditLog(
        Long id,
        // Copiado do principal autenticado no momento da ação; não aponta para a tabela de usuários.
        String actorEmail,
        AuditAction action,
        // Nome da entidade, no mesmo texto usado pelo log de cadastro ("User", "TimeSlot").
        String entityName,
        // Identificador da linha afetada; null quando a ação não aponta para um registro só.
        String entityId,
        // Frase curta escrita pelo caso de uso, já em português, porque a tela exibe direto.
        String details,
        Instant createdAt
) {

    public AuditLog {
        actorEmail = DomainValidation.requireEmail(actorEmail, "E-mail do autor é obrigatório.");
        Objects.requireNonNull(action, "Ação de auditoria é obrigatória.");
        entityName = DomainValidation.requireText(entityName, "Nome da entidade é obrigatório.");
        // Vazio vira null para a listagem não imprimir aspas em branco no lugar do id.
        entityId = entityId == null || entityId.isBlank() ? null : entityId.trim();
        details = details == null || details.isBlank() ? null : details.trim();
        Objects.requireNonNull(createdAt, "Data do log é obrigatória.");
    }

    /**
     * Linha pronta para o insert, sem id e com a data do instante informado.
     *
     * @param actorEmail quem agiu, vindo da autenticação da requisição.
     * @param action     o que aconteceu.
     * @param entityName entidade afetada.
     * @param entityId   identificador da linha, ou null para ações sem alvo único.
     * @param details    frase explicando o contexto.
     * @param now        instante da ação, injetado pelo chamador.
     * @return o log sem id, aguardando a persistência.
     */
    public static AuditLog create(String actorEmail, AuditAction action, String entityName,
                                  String entityId, String details, Instant now) {
        return new AuditLog(null, actorEmail, action, entityName, entityId, details, now);
    }

    /**
     * Copia o valor recebendo o id gerado pelo banco.
     *
     * @param newId identificador devolvido pela persistência.
     * @return a mesma linha, com id preenchido.
     */
    public AuditLog withId(Long newId) {
        return new AuditLog(newId, actorEmail, action, entityName, entityId, details, createdAt);
    }
}
