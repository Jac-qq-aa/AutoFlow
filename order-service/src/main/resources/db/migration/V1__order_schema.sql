CREATE TABLE sales_order (
    order_id VARCHAR(36) PRIMARY KEY,
    order_no VARCHAR(32) NOT NULL UNIQUE,
    channel VARCHAR(20) NOT NULL,
    channel_order_no VARCHAR(64),
    store_id VARCHAR(32) NOT NULL,
    customer_name VARCHAR(64) NOT NULL,
    customer_phone VARCHAR(32) NOT NULL,
    model_code VARCHAR(32) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    inventory_status VARCHAR(32) NOT NULL,
    payment_status VARCHAR(32) NOT NULL,
    fulfillment_status VARCHAR(32) NOT NULL,
    vin VARCHAR(32),
    version INT NOT NULL DEFAULT 0,
    created_by VARCHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_channel_order (channel, channel_order_no),
    KEY idx_store_created (store_id, created_at),
    KEY idx_status_updated (status, updated_at)
);

CREATE TABLE outbox_event (
    event_id VARCHAR(36) PRIMARY KEY,
    aggregate_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload JSON NOT NULL,
    status VARCHAR(16) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    sent_at DATETIME(3),
    KEY idx_outbox_publish (status, next_attempt_at, created_at)
);

CREATE TABLE processed_event (
    event_id VARCHAR(36) PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    processed_at DATETIME(3) NOT NULL
);


CREATE TABLE dead_letter_event (
    id VARCHAR(36) PRIMARY KEY,
    topic VARCHAR(128) NOT NULL,
    event_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(36) NOT NULL,
    event_json JSON NOT NULL,
    reason VARCHAR(2000) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    replayed_at DATETIME(3),
    KEY idx_dead_letter_status (status, created_at)
);
