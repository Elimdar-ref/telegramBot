-- Создание таблицы notification_task
CREATE TABLE notification_task (
    id BIGSERIAL PRIMARY KEY,
    chat_id BIGINT NOT NULL,
    message_text TEXT NOT NULL,
    notification_datetime TIMESTAMP NOT NULL
);