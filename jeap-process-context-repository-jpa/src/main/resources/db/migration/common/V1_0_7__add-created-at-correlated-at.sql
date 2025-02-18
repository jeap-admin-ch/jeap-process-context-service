ALTER TABLE process_instance_process_data ADD COLUMN created_at timestamp with time zone not null default now();

ALTER TABLE process_instance ADD COLUMN last_correlation_at timestamp with time zone;
