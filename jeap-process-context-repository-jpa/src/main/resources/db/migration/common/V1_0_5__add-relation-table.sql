CREATE TABLE process_instance_relations
(
    process_instance_id UUID                     NOT NULL REFERENCES process_instance (id),
    subject_type        varchar                  NOT NULL,
    subject_id          varchar                  NOT NULL,
    object_type         varchar                  NOT NULL,
    object_id           varchar                  NOT NULL,
    predicate_type      varchar                  NOT NULL,
    idempotence_id      UUID                     NOT NULL,
    created_at          timestamp with time zone NOT NULL,
    UNIQUE (process_instance_id, subject_type, subject_id, object_type, object_id, predicate_type)
);

CREATE INDEX relation_process_instance_id ON process_instance_relations (process_instance_id);
CREATE INDEX relation_idempotence_id ON process_instance_relations (idempotence_id);
