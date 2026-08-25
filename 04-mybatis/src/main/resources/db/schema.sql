CREATE DATABASE IF NOT EXISTS spring_training_mybatis
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE spring_training_mybatis;

CREATE TABLE IF NOT EXISTS tasks (
                                     id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                                     title VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'TODO',
    due_date DATE NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
    ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT chk_mybatis_tasks_status
    CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
    INDEX idx_mybatis_tasks_status_created_at (status, created_at),
    INDEX idx_mybatis_tasks_due_date (due_date)
    ) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS task_comments (
                                             id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                                             task_id BIGINT UNSIGNED NOT NULL,
                                             content VARCHAR(500) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_task_comments_task
    FOREIGN KEY (task_id) REFERENCES tasks (id)
    ON DELETE CASCADE,
    INDEX idx_task_comments_task_id_created_at (task_id, created_at)
    ) ENGINE = InnoDB;