/*
 * Os dois papéis de acesso do sistema.
 *
 * ADMIN gerencia usuários, tipos de caminhão, tipos de paletização e faixas de
 * horário, cria e cancela agendamentos, e lê a auditoria. USER tem acesso de
 * consulta: vê agendamentos, relatórios e as próprias opções de cadastro.
 *
 * Essa divisão é regra do teste técnico, não escolha de implementação, e ela é
 * aplicada em um lugar só: SecurityConfig, nas linhas que exigem ROLE_ADMIN para
 * os verbos que escrevem. Nenhum caso de uso precisa conferir papel — se a
 * requisição passou pelo filtro, a permissão já foi decidida.
 *
 * É uma enumeração e não uma tabela de permissões porque são duas funções fixas,
 * explicáveis em uma frase cada. Um terceiro papel de verdade exigiria migrar as
 * colunas que guardam o nome deste enum.
 */
package com.redekrill.scheduling.domain.model;

public enum Role {
    // Gerenciamento de cadastro, escrita em agendamentos e leitura de auditoria.
    ADMIN,
    // Consulta: agenda não se cria por conta própria neste papel.
    USER
}
