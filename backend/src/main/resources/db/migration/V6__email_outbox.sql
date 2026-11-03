-- Outbox transacional de e-mail.
--
-- A linha é criada na mesma transação do agendamento. O processador faz o
-- envio fora da transação e atualiza o estado com retry exponencial. Nenhuma
-- mensagem é removida, permitindo auditoria operacional do envio.

CREATE TABLE email_outbox (
    id              BIGSERIAL PRIMARY KEY,
    appointment_id  BIGINT       NOT NULL REFERENCES appointment (id),
    recipient       VARCHAR(160) NOT NULL,
    warnings        TEXT         NOT NULL DEFAULT '',
    status          VARCHAR(20)  NOT NULL,
    attempts        INTEGER      NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ  NOT NULL,
    last_error      VARCHAR(1000),
    created_at      TIMESTAMPTZ  NOT NULL,
    sent_at         TIMESTAMPTZ,
    CONSTRAINT ck_email_outbox_status CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    CONSTRAINT ck_email_outbox_attempts CHECK (attempts >= 0)
);

CREATE INDEX idx_email_outbox_pending
    ON email_outbox (status, next_attempt_at, created_at);

CREATE INDEX idx_email_outbox_appointment
    ON email_outbox (appointment_id);
