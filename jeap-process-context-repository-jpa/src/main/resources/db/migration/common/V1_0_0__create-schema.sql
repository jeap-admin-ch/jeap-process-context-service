CREATE TABLE process_instance
(
    id                UUID PRIMARY KEY,
    origin_process_id varchar                  NOT NULL UNIQUE,
    state             varchar                  NOT NULL,
    template_name     varchar                  NOT NULL,
    created_at        timestamp with time zone NOT NULL,
    modified_at       timestamp with time zone,
    version           integer
);

CREATE INDEX process_instance_state ON process_instance (state);
CREATE INDEX process_instance_origin_process_id ON process_instance (origin_process_id);
CREATE INDEX process_instance_created_at ON process_instance (created_at);
CREATE INDEX process_instance_modified_at ON process_instance (modified_at);
CREATE INDEX process_instance_version ON process_instance (version);

CREATE TABLE task_instance
(
    id                  UUID PRIMARY KEY,
    task_type           varchar,
    origin_task_id      varchar,
    process_instance_id UUID                     NOT NULL REFERENCES process_instance (id),
    state               varchar                  NOT NULL,
    created_at          timestamp with time zone NOT NULL,
    modified_at         timestamp with time zone,
    version             integer,
    UNIQUE (process_instance_id, origin_task_id)
);

CREATE INDEX task_instance_task_type ON task_instance (task_type);
CREATE INDEX task_instance_origin_task_id ON task_instance (origin_task_id);
CREATE INDEX task_instance_process_instance_id ON task_instance (process_instance_id);
CREATE INDEX task_instance_state ON task_instance (state);
CREATE INDEX task_instance_created_at ON task_instance (created_at);
CREATE INDEX task_instance_modified_at ON task_instance (modified_at);
CREATE INDEX task_instance_version ON task_instance (version);

CREATE TABLE milestone
(
    id                  UUID PRIMARY KEY,
    name                varchar,
    reached             bool,
    reached_at          timestamp with time zone,
    process_instance_id UUID                     NOT NULL REFERENCES process_instance (id),
    created_at          timestamp with time zone NOT NULL,
    modified_at         timestamp with time zone,
    version             integer,
    UNIQUE (name, process_instance_id)
);

CREATE INDEX milestone_process_instance_id ON milestone (process_instance_id);
CREATE INDEX milestone_version ON milestone (version);
CREATE INDEX milestone_reached ON milestone (reached);

CREATE TABLE event
(
    id                  UUID PRIMARY KEY,
    event_id            varchar,
    event_name          varchar,
    process_instance_id UUID                     NOT NULL REFERENCES process_instance (id),
    created_at          timestamp with time zone NOT NULL,
    modified_at         timestamp with time zone,
    version             integer
);

CREATE INDEX event_process_instance_id ON event (process_instance_id);
CREATE INDEX event_version ON event (version);

CREATE TABLE event_related_origin_task_ids
(
    event_id UUID    NOT NULL REFERENCES event (id),
    task_id  varchar NOT NULL
);

CREATE INDEX event_related_origin_task_ids_task_id ON event_related_origin_task_ids (task_id);

CREATE TABLE process_update
(
    id                  UUID PRIMARY KEY,
    origin_process_id   varchar                  NOT NULL,
    origin_task_id      varchar,
    process_update_type varchar                  NOT NULL,
    handled             boolean                  NOT NULL,
    event_id            varchar                  NOT NULL,
    idempotence_id      varchar                  NOT NULL,
    name                varchar,
    created_at          timestamp with time zone NOT NULL,
    modified_at         timestamp with time zone,
    version             integer,
    UNIQUE (origin_process_id, idempotence_id)
);

CREATE INDEX process_update_origin_process_id ON process_update (origin_process_id);
CREATE INDEX process_update_idempotence_id ON process_update (idempotence_id);

CREATE TABLE process_update_origin_task_ids
(
    process_update_id UUID    NOT NULL REFERENCES process_update (id),
    task_id           varchar NOT NULL
);

CREATE INDEX process_update_task_ids_process_update_id ON process_update_origin_task_ids (task_id);

CREATE TABLE process_event
(
    id                UUID PRIMARY KEY,
    origin_process_id varchar                  NOT NULL,
    event_type        varchar                  NOT NULL,
    name              varchar,
    created_at        timestamp with time zone NOT NULL,
    modified_at       timestamp with time zone,
    version           integer
);

CREATE INDEX process_event_origin_process_id ON process_event (origin_process_id);
