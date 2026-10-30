CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(160) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL
);

CREATE UNIQUE INDEX uk_users_email_lower ON users (LOWER(email));

CREATE TABLE truck_type (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(80) NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uk_truck_type_name_lower ON truck_type (LOWER(name));

CREATE TABLE palletization_type (
    id                  BIGSERIAL PRIMARY KEY,
    code                VARCHAR(30) NOT NULL UNIQUE,
    name                VARCHAR(80) NOT NULL UNIQUE,
    capacity_multiplier INTEGER     NOT NULL CHECK (capacity_multiplier >= 1)
);

CREATE TABLE time_slot (
    id         BIGSERIAL PRIMARY KEY,
    start_time TIME NOT NULL,
    end_time   TIME NOT NULL,
    CONSTRAINT uk_time_slot UNIQUE (start_time, end_time),
    CONSTRAINT ck_time_slot_order CHECK (start_time < end_time)
);

CREATE TABLE appointment (
    id                    BIGSERIAL PRIMARY KEY,
    supplier_name         VARCHAR(160) NOT NULL,
    supplier_email        VARCHAR(160) NOT NULL,
    truck_plate           VARCHAR(20)  NOT NULL,
    appointment_date      DATE         NOT NULL,
    time_slot_id          BIGINT       NOT NULL REFERENCES time_slot (id),
    truck_type_id         BIGINT       NOT NULL REFERENCES truck_type (id),
    palletization_type_id BIGINT       NOT NULL REFERENCES palletization_type (id),
    pallet_quantity       INTEGER      NOT NULL CHECK (pallet_quantity > 0),
    effective_pallets     INTEGER      NOT NULL CHECK (effective_pallets > 0),
    created_by_id         BIGINT       NOT NULL REFERENCES users (id),
    created_at            TIMESTAMPTZ  NOT NULL,
    updated_at            TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_appointment_date ON appointment (appointment_date);
CREATE INDEX idx_appointment_date_slot ON appointment (appointment_date, time_slot_id);
CREATE INDEX idx_appointment_supplier ON appointment (LOWER(supplier_name));

CREATE TABLE audit_log (
    id          BIGSERIAL PRIMARY KEY,
    actor_email VARCHAR(160) NOT NULL,
    action      VARCHAR(30)  NOT NULL,
    entity_name VARCHAR(60)  NOT NULL,
    entity_id   VARCHAR(60),
    details     TEXT,
    created_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_audit_log_created_at ON audit_log (created_at DESC);
CREATE INDEX idx_audit_log_entity ON audit_log (entity_name, entity_id);
