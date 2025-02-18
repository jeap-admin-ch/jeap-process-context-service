ALTER TABLE process_instance_process_relations ALTER COLUMN id SET NOT NULL;
ALTER TABLE process_instance_process_relations ADD PRIMARY KEY (id);

CREATE SEQUENCE events_event_data_id_seq START WITH 1 INCREMENT BY 1;
ALTER TABLE events_event_data ADD COLUMN id BIGINT NOT NULL DEFAULT NEXTVAL('events_event_data_id_seq');
ALTER TABLE events_event_data ADD PRIMARY KEY (id);

CREATE SEQUENCE events_origin_task_ids_id_seq START WITH 1 INCREMENT BY 1;
ALTER TABLE events_origin_task_ids ADD COLUMN id BIGINT NOT NULL DEFAULT NEXTVAL('events_origin_task_ids_id_seq');
ALTER TABLE events_origin_task_ids ADD PRIMARY KEY (id);
