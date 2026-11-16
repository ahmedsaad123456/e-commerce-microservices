-- one row = "the stock of this purchase was already given back"
CREATE TABLE stock_restorations (
    purchase_id UUID        PRIMARY KEY,
    restored_at TIMESTAMPTZ NOT NULL DEFAULT now()
);