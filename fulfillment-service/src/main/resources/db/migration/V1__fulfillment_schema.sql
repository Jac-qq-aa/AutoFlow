CREATE TABLE payment (
    payment_id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL UNIQUE,
    amount DECIMAL(12,2) NOT NULL,
    scenario VARCHAR(16) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    KEY idx_payment_status (status, updated_at)
);

CREATE TABLE refund (
    refund_id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL UNIQUE,
    amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL
);

CREATE TABLE delivery_task (
    task_id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL UNIQUE,
    vin VARCHAR(32) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    completed_by VARCHAR(64),
    created_at DATETIME(3) NOT NULL,
    completed_at DATETIME(3),
    KEY idx_delivery_status (status, created_at)
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
