CREATE SEQUENCE pending_message_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE pending_message
(
    id                BIGINT PRIMARY KEY,
    origin_process_id VARCHAR                  NOT NULL,
    message_id        UUID                     NOT NULL REFERENCES events (id),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (origin_process_id, message_id)
);

CREATE INDEX pending_message_origin_process_id ON pending_message (origin_process_id);
