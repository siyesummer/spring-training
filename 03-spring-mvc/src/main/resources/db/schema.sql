CREATE DATABASE IF NOT EXISTS spring_training_mvc
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE spring_training_mvc;

CREATE TABLE IF NOT EXISTS tasks (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'TODO',
    due_date DATE NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT chk_tasks_status
        CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
    INDEX idx_tasks_status_created_at (status, created_at),
    INDEX idx_tasks_due_date (due_date)
) ENGINE = InnoDB;
