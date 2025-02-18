CREATE TABLE process_instance_external_references
(
    process_instance_id UUID    NOT NULL REFERENCES process_instance (id),
    name                varchar NOT NULL,
    value               varchar NOT NULL,
    UNIQUE (process_instance_id, name, value)
);

CREATE INDEX external_references_process_instance_id ON process_instance_external_references (process_instance_id);


