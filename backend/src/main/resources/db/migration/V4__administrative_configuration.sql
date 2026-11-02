-- Etapa 1.1 - cadastros administrativos e integridade das configurações.
--
-- A tabela capacity_settings é um singleton (id = 1). Os valores iniciais
-- preservam os limites informativos definidos pelo desafio: 480 pallets por
-- dia e 120 pallets por faixa horária.
CREATE TABLE capacity_settings (
    id          SMALLINT    PRIMARY KEY,
    daily_limit INTEGER     NOT NULL,
    slot_limit  INTEGER     NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_capacity_settings_singleton CHECK (id = 1),
    CONSTRAINT ck_capacity_settings_daily_limit
        CHECK (daily_limit BETWEEN 1 AND 1000000),
    CONSTRAINT ck_capacity_settings_slot_limit
        CHECK (slot_limit BETWEEN 1 AND 1000000)
);

INSERT INTO capacity_settings (id, daily_limit, slot_limit, updated_at)
VALUES (1, 480, 120, NOW());

-- As faixas existentes continuam com os mesmos IDs. As constraints abaixo
-- impedem novos horários inválidos, mas não alteram nem removem registros.
ALTER TABLE time_slot
    ADD CONSTRAINT ck_time_slot_start_before_end CHECK (start_time < end_time);

ALTER TABLE time_slot
    ADD CONSTRAINT ck_time_slot_two_hours
    CHECK (end_time = start_time + INTERVAL '2 hours');

CREATE UNIQUE INDEX uk_time_slot_start_time ON time_slot (start_time);

-- Os dois tipos obrigatórios mantêm seus multiplicadores no próprio banco.
-- Tipos adicionais, se cadastrados pelo ADMIN, ficam limitados a 100x para
-- evitar overflow no cálculo da quantidade efetiva.
ALTER TABLE palletization_type
    ADD CONSTRAINT ck_palletization_mandatory_multiplier CHECK (
        (code = 'PALETIZADO' AND capacity_multiplier = 1)
        OR (code = 'NAO_PALETIZADO' AND capacity_multiplier = 2)
        OR (
            code NOT IN ('PALETIZADO', 'NAO_PALETIZADO')
            AND capacity_multiplier BETWEEN 1 AND 100
        )
    );

CREATE UNIQUE INDEX uk_palletization_type_name_lower
    ON palletization_type (LOWER(name));
