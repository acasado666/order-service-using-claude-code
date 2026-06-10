--liquibase formatted sql

--changeset antonio:002-create-customers-table
CREATE TABLE customers (
    id          UUID         NOT NULL,
    email       VARCHAR(255) NOT NULL,
    full_name   VARCHAR(255) NOT NULL,
    phone       VARCHAR(50),
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,
    CONSTRAINT pk_customers PRIMARY KEY (id),
    CONSTRAINT uq_customers_email UNIQUE (email)
);

CREATE INDEX idx_customers_email ON customers (email);
