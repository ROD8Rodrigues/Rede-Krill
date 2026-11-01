-- Etapa 1 - conformidade funcional do agendamento.
--
-- Estratégia de migração segura:
-- 1. As novas colunas são criadas de forma compatível com registros existentes.
-- 2. Registros legados sem ID Pedido recebem um identificador reservado na faixa
--    9.000.000.000.000.000.000+ (BIGINT), evitando colisão com IDs reais e
--    preservando o histórico. O valor nunca é reutilizado.
-- 3. Só depois do backfill a coluna order_id se torna NOT NULL e recebe índice único.
-- 4. Limites de 80 caracteres são aplicados com CHECK NOT VALID para não invalidar
--    registros antigos; novos registros e atualizações passam a respeitá-los.
-- 5. Nenhum registro é removido ou truncado.

ALTER TABLE appointment ADD COLUMN order_id BIGINT;
ALTER TABLE appointment ADD COLUMN notes VARCHAR(500);
ALTER TABLE appointment ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE appointment ADD COLUMN canceled_at TIMESTAMPTZ;
ALTER TABLE appointment ADD COLUMN canceled_by_id BIGINT REFERENCES users (id);

DO $$
DECLARE
    next_order_id BIGINT;
    legacy_row RECORD;
BEGIN
    SELECT GREATEST(
               900000000000000000,
               COALESCE(MAX(order_id), 0) + 1
           )
      INTO next_order_id
      FROM appointment;

    FOR legacy_row IN
        SELECT id FROM appointment WHERE order_id IS NULL ORDER BY id
    LOOP
        UPDATE appointment SET order_id = next_order_id WHERE id = legacy_row.id;
        next_order_id := next_order_id + 1;
    END LOOP;
END $$;

ALTER TABLE appointment ALTER COLUMN order_id SET NOT NULL;

CREATE UNIQUE INDEX uk_appointment_order_id ON appointment (order_id);
CREATE INDEX idx_appointment_status_date ON appointment (status, appointment_date);
CREATE INDEX idx_appointment_canceled_at ON appointment (canceled_at);

ALTER TABLE appointment
    ADD CONSTRAINT ck_appointment_order_id_positive CHECK (order_id > 0);

ALTER TABLE appointment
    ADD CONSTRAINT ck_appointment_status CHECK (status IN ('ACTIVE', 'CANCELED'));

ALTER TABLE appointment
    ADD CONSTRAINT ck_appointment_cancellation CHECK (
        (status = 'ACTIVE' AND canceled_at IS NULL AND canceled_by_id IS NULL)
        OR
        (status = 'CANCELED' AND canceled_at IS NOT NULL AND canceled_by_id IS NOT NULL)
    );

-- NOT VALID: preserva registros legados acima do limite, mas impede novos
-- cadastros/atualizações fora do padrão definido pelo desafio.
ALTER TABLE appointment
    ADD CONSTRAINT ck_appointment_supplier_name_length
    CHECK (char_length(supplier_name) <= 80) NOT VALID;

ALTER TABLE appointment
    ADD CONSTRAINT ck_appointment_supplier_email_length
    CHECK (char_length(supplier_email) <= 80) NOT VALID;

-- Tipo de caminhão previsto no desafio e ausente no seed original.
INSERT INTO truck_type (name, description, created_at, updated_at)
SELECT 'Van', 'Van de carga', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM truck_type WHERE LOWER(name) = LOWER('Van')
);
