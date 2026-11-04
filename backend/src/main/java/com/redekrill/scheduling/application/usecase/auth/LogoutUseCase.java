/*
 * O lado auditável do logout.
 *
 * Esta classe só grava a linha de LOGOUT — e é deliberadamente pouco, porque a
 * revogação efetiva é de responsabilidade de AuthSessionService. O controller pega o
 * cookie de refresh e o atributo de sessão da requisição, revoga pelos dois caminhos e
 * depois chama este caso de uso. O motivo da separação é o erro esperado: uma sessão
 * pode já ter sido derrubada por rotação, e a auditoria não deve falhar junto.
 *
 * Por isso o "JWT é stateless" não é o fim da história aqui: com a sessão revogada no
 * servidor, o access token deixa de passar no filtro mesmo nas mãos de quem o roubou.
 */
package com.redekrill.scheduling.application.usecase.auth;

import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.domain.exception.AuthenticationFailedException;
import com.redekrill.scheduling.domain.model.AuditAction;

/**
 * Registra o logout na trilha de auditoria. O token JWT é stateless e deixa de
 * ser aceito quando o cliente o descarta.
 */
public class LogoutUseCase {

    private final AuditService auditService;

    public LogoutUseCase(AuditService auditService) {
        this.auditService = auditService;
    }

    public void execute(String actorEmail) {
        if (actorEmail == null || actorEmail.isBlank()) {
            throw new AuthenticationFailedException("Usuário autenticado não identificado.");
        }
        auditService.record(actorEmail, AuditAction.LOGOUT, "User", null, "Logout realizado.");
    }
}
