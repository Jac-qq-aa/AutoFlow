CREATE TABLE sales_order_record (
    record_id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    store_id VARCHAR(32) NOT NULL,
    record_type VARCHAR(32) NOT NULL,
    source_page VARCHAR(64) NOT NULL,
    record_data JSON NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    KEY idx_order_record (order_id, created_at),
    KEY idx_store_record_type (store_id, record_type, created_at),
    CONSTRAINT fk_record_order FOREIGN KEY (order_id) REFERENCES sales_order(order_id)
);
