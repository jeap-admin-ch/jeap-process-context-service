-- new table for storing all events the PCS is interested in
CREATE TABLE events
(
    id               UUID PRIMARY KEY,
    event_id         varchar                  NOT NULL,
    event_name       varchar                  NOT NULL,
    idempotence_id   varchar,
    created_at       timestamp with time zone NOT NULL
);
CREATE INDEX events_event_name ON events (event_name);
CREATE INDEX events_idempotence_id ON events (idempotence_id);
CREATE INDEX events_event_id ON events (event_id);


-- Migrate the events already associated with process instances to the new events table
INSERT INTO events (id, event_id, event_name, idempotence_id, created_at)
SELECT (uuid_in(md5(random()::text || clock_timestamp()::text)::cstring)) as id, event_id, event_name, NULL as idempotence_id, MIN(created_at) as created_at
FROM event
group by event_id, event_name;

-- New table for storing all event data extracted from an event
CREATE TABLE events_event_data
(
    events_id        UUID    NOT NULL REFERENCES events (id),
    template_name    varchar NOT NULL,
    key              varchar NOT NULL,
    value            varchar NOT NULL,
    role             varchar
);
CREATE INDEX event_data_events_id ON events_event_data (events_id);

-- Migrate event data already associated with process instances to the new event data table.
INSERT INTO events_event_data(events_id, template_name, key, value, role)
SELECT es.id as events_id, p.template_name as template_name, eed.key as key, eed.value as value, eed.role as role
FROM process_instance p
         INNER JOIN event e ON e.process_instance_id = p.id
         INNER JOIN event_event_data eed ON eed.event_id = e.id
         INNER JOIN events es ON es.event_id = e.event_id AND es.event_name = e.event_name
group by es.id, p.template_name, eed.key, eed.value, eed.role;
DROP TABLE event_event_data;

-- New table for storing all task ids extracted from an event
CREATE TABLE events_origin_task_ids
(
    events_id      UUID    NOT NULL REFERENCES events (id),
    template_name  varchar NOT NULL,
    origin_task_id varchar NOT NULL
);
CREATE INDEX origin_task_ids_events_id ON events_event_data (events_id);

-- Migrate task ids already associated with process instances to the new task ids table.
INSERT INTO events_origin_task_ids(events_id, template_name, origin_task_id)
SELECT es.id as events_id, p.template_name as template_name, et.task_id as origin_task_id
FROM process_instance p
         INNER JOIN event e ON e.process_instance_id = p.id
         INNER JOIN event_related_origin_task_ids et ON et.event_id = e.id
         INNER JOIN events es ON es.event_id = e.event_id AND es.event_name = e.event_name
group by es.id, p.template_name, et.task_id;
DROP TABLE event_related_origin_task_ids;

-- Extend the process update table with an explicit event_name field
ALTER TABLE process_update ADD COLUMN event_name varchar;

-- Populate event_name field in process updates according to the process_update_event_type
UPDATE process_update
SET event_name = name
WHERE process_update_type = 'DOMAIN_EVENT';

UPDATE process_update
SET event_name = 'ProcessTaskPlannedEvent'
WHERE process_update_type = 'TASK_PLANNED';

UPDATE process_update
SET event_name = 'ProcessTaskCompletedEvent'
WHERE process_update_type = 'TASK_COMPLETED';
CREATE INDEX process_update_event_name ON process_update (event_name);

ALTER TABLE process_update DROP CONSTRAINT process_update_origin_process_id_idempotence_id_key;
CREATE UNIQUE INDEX process_update_origin_process_id_event_name_idempotence_id_key
    ON process_update (origin_process_id, event_name, idempotence_id);

-- Create events from not yet handled process updates
INSERT INTO events (id, event_id, event_name, idempotence_id, created_at)
SELECT (uuid_in(md5(random()::text || clock_timestamp()::text)::cstring)) as id, event_id, event_name, idempotence_id, created_at as created_at
FROM process_update
where handled = false
group by event_id, event_name, idempotence_id, created_at;

-- Migrate event data associated with not yet handled process updates to the just created events
INSERT INTO events_origin_task_ids(events_id, template_name, origin_task_id)
SELECT es.id as events_id, p.template_name as template_name, ut.task_id as origin_task_id
FROM process_instance p
         INNER JOIN process_update u ON u.origin_process_id = p.origin_process_id
         INNER JOIN process_update_origin_task_ids ut ON ut.process_update_id = u.id
         INNER JOIN events es ON es.event_id = u.event_id AND es.event_name = u.event_name
where u.handled = false
group by es.id, p.template_name, ut.task_id;
DROP TABLE process_update_origin_task_ids;

-- Migrate task ids associated with not yet handled process updates to the just created events
INSERT INTO events_event_data(events_id, template_name, key, value, role)
SELECT es.id as events_id, p.template_name as template_name, ued.key as key, ued.value as value, ued.role as role
FROM process_instance p
         INNER JOIN process_update u ON u.origin_process_id = p.origin_process_id
         INNER JOIN process_update_event_data ued ON ued.process_update_id = u.id
         INNER JOIN events es ON es.event_id = u.event_id AND es.event_name = u.event_name
where u.handled = false
group by es.id, p.template_name, ued.key, ued.value, ued.role;
DROP TABLE process_update_event_data;


-- Extend each process update with a link to an event
ALTER TABLE process_update ADD COLUMN event_reference UUID;

-- Link each process update to its corresponding event
UPDATE process_update u
SET event_reference = (
    SELECT id FROM events e WHERE u.event_id = e.event_id AND u.event_name = e.event_name
                              AND u.idempotence_id = e.idempotence_id AND u.created_at = e.created_at
)
where handled = false;

-- -- Fill missing idempotence ids in the events table with the corresponding idempotence id of process updates
UPDATE events es
SET idempotence_id = (
    SELECT DISTINCT idempotence_id FROM process_update u WHERE u.event_id = es.event_id AND u.event_name = es.event_name
)
where idempotence_id is NULL;

-- Drop no longer needed domain event id field from process update.
ALTER TABLE process_update DROP COLUMN event_id;

-- Create table to store event references associated with a process instance
CREATE TABLE event_reference
(
    id                    UUID PRIMARY KEY,
    events_id              UUID                     NOT NULL,
    process_instance_id   UUID                     NOT NULL REFERENCES process_instance (id),
    created_at            timestamp with time zone NOT NULL
);
CREATE INDEX event_reference_process_instance_id ON event_reference (process_instance_id);

-- Associate process instances with their matching events migrated to the events table.
INSERT INTO event_reference (id, events_id, process_instance_id, created_at)
SELECT (uuid_in(md5(random()::text || clock_timestamp()::text)::cstring)) as id, es.id as events_id, p.id as process_instance_id, CURRENT_TIMESTAMP as created_at
FROM process_instance p
INNER JOIN event e ON e.process_instance_id = p.id
INNER JOIN events es ON es.event_id = e.event_id AND es.event_name = e.event_name
group by p.id, es.id;
DROP TABLE event;
