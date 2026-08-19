CREATE DATABASE IF NOT EXISTS spring_training_core
       CHARACTER SET utf8mb4
       COLLATE utf8mb4_unicode_ci;

USE spring_training_core;

CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_name VARCHAR(50) NOT NULL,
    balance DECIMAL(12, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_accounts_balance CHECK (balance >= 0)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS transfer_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    from_account_id BIGINT NOT NULL,
    to_account_id BIGINT NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_transfer_logs_amount CHECK (amount > 0),
    CONSTRAINT fk_transfer_logs_from_account
        FOREIGN KEY (from_account_id) REFERENCES accounts (id),
    CONSTRAINT fk_transfer_logs_to_account
        FOREIGN KEY (to_account_id) REFERENCES accounts (id)
) ENGINE = InnoDB;

INSERT INTO accounts (id, owner_name, balance)
VALUES
    (1, '青木', 1000.00),
    (2, '四叶', 500.00);

DELETE FROM transfer_logs;
UPDATE accounts SET balance = 1000.00 WHERE id = 1;
UPDATE accounts SET balance = 500.00 WHERE id = 2;

SELECT * FROM accounts;

SELECT * FROM transfer_logs;
