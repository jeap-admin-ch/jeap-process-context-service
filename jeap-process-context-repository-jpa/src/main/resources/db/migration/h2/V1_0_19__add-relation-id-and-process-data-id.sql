ALTER TABLE process_instance_relations
ADD COLUMN id UUID;

UPDATE process_instance_relations
SET id = RANDOM_UUID()
WHERE id is NULL;

ALTER TABLE process_instance_relations
ALTER COLUMN id SET NOT NULL;

ALTER TABLE process_instance_relations
ADD CONSTRAINT pk_process_instance_relations PRIMARY KEY (id);

ALTER TABLE process_instance_process_data
ADD COLUMN id UUID;

UPDATE process_instance_process_data
SET id = RANDOM_UUID()
WHERE id is NULL;

ALTER TABLE process_instance_process_data
ALTER COLUMN id SET NOT NULL;

ALTER TABLE process_instance_process_data
ADD CONSTRAINT pk_process_instance_process_data PRIMARY KEY (id);
