CREATE TABLE inventory_quota (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id VARCHAR(32) NOT NULL,
    model_code VARCHAR(32) NOT NULL,
    available INT NOT NULL,
    reserved INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_store_model (store_id, model_code),
    CONSTRAINT chk_available_nonnegative CHECK (available >= 0),
    CONSTRAINT chk_reserved_nonnegative CHECK (reserved >= 0)
);

CREATE TABLE vehicle (
    vin VARCHAR(32) PRIMARY KEY,
    store_id VARCHAR(32) NOT NULL,
    model_code VARCHAR(32) NOT NULL,
    color VARCHAR(32) NOT NULL,
    status VARCHAR(20) NOT NULL,
    allocated_order_id VARCHAR(36),
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_allocated_order (allocated_order_id),
    KEY idx_sellable_vehicle (store_id, model_code, status)
);

CREATE TABLE inventory_reservation (
    reservation_id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL UNIQUE,
    store_id VARCHAR(32) NOT NULL,
    model_code VARCHAR(32) NOT NULL,
    vin VARCHAR(32),
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_reservation_vin (vin)
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
INSERT INTO inventory_quota(store_id, model_code, available, reserved, version, updated_at) VALUES
('STORE-SH-001','AF-SUV-PRO',8,0,0,CURRENT_TIMESTAMP),
('STORE-SH-001','AF-SEDAN-X',7,0,0,CURRENT_TIMESTAMP),
('STORE-BJ-001','AF-SUV-PRO',5,0,0,CURRENT_TIMESTAMP),
('STORE-BJ-001','AF-CITY-EV',4,0,0,CURRENT_TIMESTAMP),
('STORE-SZ-001','AF-SEDAN-X',3,0,0,CURRENT_TIMESTAMP),
('STORE-SZ-001','AF-CITY-EV',3,0,0,CURRENT_TIMESTAMP);

INSERT INTO vehicle(vin, store_id, model_code, color, status, updated_at) VALUES
('LAF00000000000001','STORE-SH-001','AF-SUV-PRO','曜石黑','AVAILABLE',CURRENT_TIMESTAMP),
('LAF00000000000002','STORE-SH-001','AF-SUV-PRO','云雾白','AVAILABLE',CURRENT_TIMESTAMP),
('LAF00000000000003','STORE-SH-001','AF-SUV-PRO','星空灰','AVAILABLE',CURRENT_TIMESTAMP),
('LAF00000000000004','STORE-SH-001','AF-SEDAN-X','冰川蓝','AVAILABLE',CURRENT_TIMESTAMP),
('LAF00000000000005','STORE-SH-001','AF-SEDAN-X','云雾白','AVAILABLE',CURRENT_TIMESTAMP),
('LAF00000000000006','STORE-BJ-001','AF-SUV-PRO','曜石黑','AVAILABLE',CURRENT_TIMESTAMP),
('LAF00000000000007','STORE-BJ-001','AF-SUV-PRO','云雾白','AVAILABLE',CURRENT_TIMESTAMP),
('LAF00000000000008','STORE-BJ-001','AF-CITY-EV','薄荷绿','AVAILABLE',CURRENT_TIMESTAMP),
('LAF00000000000009','STORE-SZ-001','AF-SEDAN-X','冰川蓝','AVAILABLE',CURRENT_TIMESTAMP),
('LAF00000000000010','STORE-SZ-001','AF-CITY-EV','薄荷绿','AVAILABLE',CURRENT_TIMESTAMP);
