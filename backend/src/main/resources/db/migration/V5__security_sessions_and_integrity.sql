-- Etapa 2 - segurança de sessão, troca obrigatória de senha e controle de concorrência.
--
-- Nenhum registro existente é removido. As colunas novas possuem default seguro
-- para permitir a atualização de bancos já populados.

ALTER TABLE users
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE appointment
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE auth_session (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    refresh_token_hash VARCHAR(64)  NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL,
    expires_at         TIMESTAMPTZ  NOT NULL,
    revoked_at         TIMESTAMPTZ,
    ip_address         VARCHAR(64),
    user_agent         VARCHAR(255),
    CONSTRAINT uk_auth_session_refresh_hash UNIQUE (refresh_token_hash)
);

CREATE INDEX idx_auth_session_user ON auth_session (user_id);
CREATE INDEX idx_auth_session_expires ON auth_session (expires_at);
CREATE INDEX idx_auth_session_active ON auth_session (user_id, revoked_at, expires_at);

-- A tabela capacity_settings nunca foi lida pela aplicação. Os limites 480/120
-- são regras fixas do desafio, documentadas em ADR-004. A remoção evita uma
-- configuração aparente que não surtiria efeito.
DROP TABLE IF EXISTS capacity_settings;

-- Permite mais de um tipo adicional (code = OUTRO). Os dois tipos obrigatórios
-- continuam únicos por code e com multiplicadores validados pela constraint.
ALTER TABLE palletization_type DROP CONSTRAINT IF EXISTS palletization_type_code_key;
