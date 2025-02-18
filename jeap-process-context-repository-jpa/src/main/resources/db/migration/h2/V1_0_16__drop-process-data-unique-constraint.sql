drop table process_instance_process_data;

CREATE TABLE process_instance_process_data
(
    process_instance_id UUID  NOT NULL REFERENCES process_instance (id),
    key               varchar NOT NULL,
    value             varchar NOT NULL,
    role              varchar,
    created_at timestamp with time zone not null default now()
);

CREATE INDEX process_data_process_instance_id ON process_instance_process_data (process_instance_id);
CREATE INDEX process_data_role ON process_instance_process_data (role);
CREATE INDEX process_instance_process_data_key ON process_instance_process_data (key);
CREATE INDEX process_instance_process_data_value ON process_instance_process_data (value);
