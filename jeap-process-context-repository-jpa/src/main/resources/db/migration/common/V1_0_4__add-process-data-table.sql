CREATE TABLE process_instance_process_data
(
    process_instance_id UUID  NOT NULL REFERENCES process_instance (id),
    key               varchar NOT NULL,
    value             varchar NOT NULL,
    role              varchar,
    UNIQUE (process_instance_id, key, value)
);

CREATE INDEX process_data_process_instance_id ON process_instance_process_data (process_instance_id);
CREATE INDEX process_data_role ON process_instance_process_data (role);

ALTER TABLE event_event_data ADD COLUMN role varchar;
ALTER TABLE process_update_event_data ADD COLUMN role varchar;


