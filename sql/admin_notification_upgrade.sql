CREATE TABLE IF NOT EXISTS notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type VARCHAR(64) NOT NULL,
    title VARCHAR(128) NOT NULL,
    content VARCHAR(255) NOT NULL,
    is_read TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_notification_user_time (user_id, create_time),
    KEY idx_notification_user_read (user_id, is_read)
);
