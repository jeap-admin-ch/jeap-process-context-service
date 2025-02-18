CREATE INDEX events_created_at ON events (created_at);
CREATE INDEX event_reference_events_id ON event_reference (events_id);
CREATE INDEX process_update_created_at ON process_update (created_at);
CREATE INDEX process_update_handled ON process_update (handled);