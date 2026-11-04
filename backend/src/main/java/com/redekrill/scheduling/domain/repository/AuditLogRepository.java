/*
 * Contrato de grava e leitura da trilha de auditoria.
 *
 * São dois métodos, e a ausência dos outros é a regra. Não existe update nem
 * delete aqui: linha de auditoria que se reescreve deixa de servir como prova. O
 * que se faz quando um fato muda é registrar a linha nova, ao lado da antiga.
 *
 * A listagem não tem filtro por autor, por ação nem por período. Isso é escolha
 * de escopo, não esquecimento: a rota exposta hoje é a consulta administrativa
 * completa, paginada, e um filtro por quem agiu só deveria ser acrescentado com o
 * cuidado de não virar ferramenta para descobrir senha nem e-mail de terceiros
 * além do que a tela já mostra para o ADMIN.
 */
package com.redekrill.scheduling.domain.repository;

import com.redekrill.scheduling.domain.model.AuditLog;
import com.redekrill.scheduling.domain.model.PageResult;

/**
 * Append-only: grava a linha e devolve a página consultada pelo ADMIN.
 *
 * O que esta interface faz: descreve o mínimo que a auditoria precisa para existir
 * como tabela consultável.
 *
 * O que ela NÃO faz: expor edição, exclusão ou filtro por autor — quem precisar
 * disso vai mudar o contrato, e essa mudança merece um teste antes.
 */
public interface AuditLogRepository {

    AuditLog save(AuditLog auditLog);

    PageResult<AuditLog> findAll(int page, int size);
}
