CREATE SEQUENCE events_user_data_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE events_user_data
(
    id        BIGINT  NOT NULL DEFAULT NEXTVAL('events_user_data_id_seq') PRIMARY KEY,
    events_id UUID    NOT NULL REFERENCES events (id),
    key_      varchar NOT NULL,
    value_    varchar NOT NULL
);

CREATE INDEX user_data_events_id ON events_user_data (events_id);

