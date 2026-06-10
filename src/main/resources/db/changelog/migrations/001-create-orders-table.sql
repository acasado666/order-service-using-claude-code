-- liquibase formatted sql

-- changeset skmcore:001-create-orders
CREATE TABLE orders
(
    id           UUID           NOT NULL,
    customer_id  VARCHAR(255)   NOT NULL,
    status       VARCHAR(50)    NOT NULL,
    total_amount NUMERIC(19, 4) NOT NULL,
    created_at   TIMESTAMP      NOT NULL,
    updated_at   TIMESTAMP      NOT NULL,
    CONSTRAINT pk_orders PRIMARY KEY (id)
);

CREATE INDEX idx_orders_customer_id ON orders (customer_id);
CREATE INDEX idx_orders_status ON orders (status);

-- changeset skmcore:002-create-order-items
CREATE TABLE order_items
(
    id           UUID           NOT NULL,
    order_id     UUID           NOT NULL,
    product_id   VARCHAR(255)   NOT NULL,
    product_name VARCHAR(255)   NOT NULL,
    quantity     INTEGER        NOT NULL,
    unit_price   NUMERIC(19, 4) NOT NULL,
    subtotal     NUMERIC(19, 4) NOT NULL,
    CONSTRAINT pk_order_items PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
