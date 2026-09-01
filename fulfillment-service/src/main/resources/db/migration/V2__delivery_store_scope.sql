ALTER TABLE delivery_task
    ADD COLUMN store_id VARCHAR(32) NULL AFTER order_id,
    ADD KEY idx_delivery_store_status (store_id, status, created_at);
