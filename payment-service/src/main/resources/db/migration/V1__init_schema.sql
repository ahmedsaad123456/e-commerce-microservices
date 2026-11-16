CREATE TABLE payments (
    id              UUID           PRIMARY KEY,
    order_reference VARCHAR(64)    NOT NULL,
    customer_id     VARCHAR(64)    NOT NULL,
    amount          NUMERIC(14, 2) NOT NULL,
    status          VARCHAR(20)    NOT NULL,          -- PROCESSING, SUCCEEDED, FAILED
    reason          VARCHAR(200),
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_payments_amount CHECK (amount > 0),
    -- one payment per order, enforced by the database itself
    CONSTRAINT uq_payments_order_reference UNIQUE (order_reference)
);