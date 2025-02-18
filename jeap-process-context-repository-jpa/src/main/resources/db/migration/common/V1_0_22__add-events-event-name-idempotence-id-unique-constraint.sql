ALTER TABLE events ADD CONSTRAINT events_event_name_idempotence_id_unique UNIQUE(event_name, idempotence_id);
