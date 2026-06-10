--liquibase formatted sql

--changeset antonio:003-update-orders-schema
DROP INDEX idx_orders_customer_id;
ALTER TABLE orders DROP COLUMN customer_id;

ALTER TABLE orders ADD COLUMN customer_id  UUID         NOT NULL;
ALTER TABLE orders ADD COLUMN order_number VARCHAR(20)  NOT NULL;
ALTER TABLE orders ADD COLUMN version      BIGINT       DEFAULT 0;
ALTER TABLE orders ADD COLUMN street       VARCHAR(255) NOT NULL;
ALTER TABLE orders ADD COLUMN city         VARCHAR(255) NOT NULL;
ALTER TABLE orders ADD COLUMN state        VARCHAR(100) NOT NULL;
ALTER TABLE orders ADD COLUMN zip_code     VARCHAR(20)  NOT NULL;
ALTER TABLE orders ADD COLUMN country      VARCHAR(100) NOT NULL;

ALTER TABLE orders ALTER COLUMN total_amount TYPE NUMERIC(12, 2);
ALTER TABLE order_items ALTER COLUMN unit_price TYPE NUMERIC(10, 2);
ALTER TABLE order_items ALTER COLUMN subtotal   TYPE NUMERIC(12, 2);

ALTER TABLE orders ADD CONSTRAINT fk_orders_customer
    FOREIGN KEY (customer_id) REFERENCES customers (id);
ALTER TABLE orders ADD CONSTRAINT uq_orders_order_number UNIQUE (order_number);

CREATE INDEX idx_orders_customer_id ON orders (customer_id);
