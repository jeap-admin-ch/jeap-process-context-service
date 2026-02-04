CREATE INDEX CONCURRENTLY idx_process_update_not_handled ON process_update (origin_process_id, handled)
    WHERE handled = false;
