DROP TABLE process_instance_process_relations;

CREATE TABLE process_instance_process_relations
(
    id                  UUID,
    process_instance_id UUID                     NOT NULL REFERENCES process_instance (id),
    name                varchar                  NOT NULL,
    role_type           varchar                  NOT NULL,
    origin_role         varchar                  NOT NULL,
    target_role         varchar                  NOT NULL,
    visibility_type     varchar                  NOT NULL,
    related_process_id  varchar                  NOT NULL,
    created_at          timestamp with time zone NOT NULL
);

CREATE INDEX process_instance_process_relations_id ON process_instance_process_relations (process_instance_id);
CREATE INDEX process_instance_process_relations_related_process_id ON process_instance_process_relations (related_process_id);