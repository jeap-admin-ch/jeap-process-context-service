ALTER TABLE process_instance ADD COLUMN latest_snapshot_version integer NOT NULL default 0;
ALTER TABLE process_instance ADD COLUMN snapshot_names varchar default '';