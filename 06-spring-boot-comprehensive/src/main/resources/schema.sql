CREATE DATABASE IF NOT EXISTS spring_training_comprehensive
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE spring_training_comprehensive;

CREATE TABLE products (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          name VARCHAR(100) NOT NULL,
                          price DECIMAL(10, 2) NOT NULL,
                          enabled TINYINT NOT NULL DEFAULT 1,
                          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE inventories (
                             product_id BIGINT PRIMARY KEY,
                             available_quantity INT NOT NULL,
                             version INT NOT NULL DEFAULT 0,
                             CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES products(id),
                             CONSTRAINT ck_inventory_quantity CHECK (available_quantity >= 0)
);

CREATE TABLE orders (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        buyer_id BIGINT NOT NULL,
                        total_amount DECIMAL(12, 2) NOT NULL,
                        status VARCHAR(20) NOT NULL,
                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        cancelled_at DATETIME NULL,
                        INDEX idx_orders_buyer_created (buyer_id, created_at)
);

CREATE TABLE order_items (
                             id BIGINT PRIMARY KEY AUTO_INCREMENT,
                             order_id BIGINT NOT NULL,
                             product_id BIGINT NOT NULL,
                             quantity INT NOT NULL,
                             unit_price DECIMAL(10, 2) NOT NULL,
                             CONSTRAINT fk_item_order FOREIGN KEY (order_id) REFERENCES orders(id),
                             CONSTRAINT fk_item_product FOREIGN KEY (product_id) REFERENCES products(id),
                             CONSTRAINT uk_order_product UNIQUE (order_id, product_id),
                             CONSTRAINT ck_item_quantity CHECK (quantity > 0)
);

CREATE TABLE operation_logs (
                                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                order_id BIGINT NOT NULL,
                                action VARCHAR(30) NOT NULL,
                                detail VARCHAR(500) NULL,
                                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                INDEX idx_logs_order_created (order_id, created_at),
                                CONSTRAINT fk_log_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

INSERT INTO products (name, price, enabled)
VALUES ('机械键盘', 299.00, 1), ('USB-C 扩展坞', 159.00, 1);

INSERT INTO inventories (product_id, available_quantity)
VALUES (1, 10), (2, 5);