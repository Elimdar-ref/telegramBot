--liquibase форматированный sql

--changeset elimdar

-- Создание таблицы Notification_task
CREATE TABLE notification_task (
    id BIGSERIAL PRIMARY KEY,
    chat_id BIGINT NOT NULL,
    message_text TEXT NOT NULL,
    notification_date_time TIMESTAMP NOT NULL
);