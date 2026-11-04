/*
 * O verbo da linha de auditoria.
 *
 * A lista é maior do que CRUD porque metade do que interessa num incidente não é
 * escrita em tabela: uma senha errada, um token reapresentado, uma rota negada.
 * Esses eventos não mexem em nenhuma tabela de negócio — a única gravação é a
 * própria linha de auditoria — e justamente por isso precisam de registro
 * próprio: senão a prova de uma tentativa ficaria só no arquivo de log do
 * servidor, fora do alcance da tela que o ADMIN consulta.
 *
 * Os nomes são gravados como texto na coluna action do log. Adicionar um valor é
 * seguro; renomear um existente quebra o histórico consultado por quem busca o
 * nome antigo.
 */
package com.redekrill.scheduling.domain.model;

public enum AuditAction {
    // Registro criado — agendamento, usuário, tipo de caminhão, faixa, tipo de paletização.
    CREATE,
    // Registro alterado por quem tem permissão de escrita.
    UPDATE,
    // Registro de cadastro excluído; agendamento nunca chega aqui, vai para CANCEL.
    DELETE,
    // Cancelamento lógico de agendamento, com quem cancelou e quando.
    CANCEL,
    // Login aceito, com a sessão correspondente aberta em seguida.
    LOGIN,
    // Senha inválida ou conta inexistente/inativa; as duas causas têm textos diferentes.
    LOGIN_FAILED,
    // Tentativa recusada pelo bloqueio temporário, antes de qualquer comparação de senha.
    LOGIN_BLOCKED,
    // Encerramento de sessão pelo próprio usuário.
    LOGOUT,
    // Alteração de senha feita pelo dono da conta — e também a recusa, quando a senha atual não confere.
    PASSWORD_CHANGE,
    // Refresh token reapresentado depois de consumido; a família inteira de sessões é revogada.
    REFRESH_TOKEN_REUSE,
    // Rota exigindo papel que o chamador não tem, ou chamada sem autenticação.
    ACCESS_DENIED
}
