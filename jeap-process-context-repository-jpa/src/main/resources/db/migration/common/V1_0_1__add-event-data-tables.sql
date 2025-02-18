CREATE TABLE event_event_data
(
    event_id UUID    NOT NULL REFERENCES event (id),
    key      varchar NOT NULL,
    value    varchar NOT NULL
);

CREATE INDEX event_event_data_event_event_data ON event_event_data (event_id);

CREATE TABLE process_update_event_data
(
    process_update_id UUID    NOT NULL REFERENCES process_update (id),
    key               varchar NOT NULL,
    value             varchar NOT NULL
);

CREATE INDEX process_update_event_data_process_update_id ON process_update_event_data (process_update_id);
