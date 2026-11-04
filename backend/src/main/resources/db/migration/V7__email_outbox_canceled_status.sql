-- Status CANCELED no outbox de e-mail.
--
-- Quando o agendamento é cancelado antes do envio, a confirmação não deve chegar
-- ao fornecedor. A mensagem recebe o status terminal CANCELED em vez de ser
-- removida, mantendo a regra do V6 de que nenhuma mensagem sai do histórico.

ALTER TABLE email_outbox DROP CONSTRAINT ck_email_outbox_status;

ALTER TABLE email_outbox ADD CONSTRAINT ck_email_outbox_status
    CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'CANCELED'));
