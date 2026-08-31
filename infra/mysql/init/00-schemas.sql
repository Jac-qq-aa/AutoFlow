CREATE DATABASE IF NOT EXISTS autoflow_order CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS autoflow_inventory CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS autoflow_fulfillment CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS 'autoflow'@'%' IDENTIFIED BY 'autoflow123';
GRANT ALL PRIVILEGES ON autoflow_order.* TO 'autoflow'@'%';
GRANT ALL PRIVILEGES ON autoflow_inventory.* TO 'autoflow'@'%';
GRANT ALL PRIVILEGES ON autoflow_fulfillment.* TO 'autoflow'@'%';
FLUSH PRIVILEGES;

