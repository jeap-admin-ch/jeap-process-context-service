-- CREATE INDEX CONCURRENTLY is a non-transactional flyway change and is thus separated from V1_0_42 (duplicate cleanup)
CREATE UNIQUE INDEX CONCURRENTLY idx_process_data_unique
    ON process_instance_process_data
        (process_instance_id, key_, value_, role)
    NULLS NOT DISTINCT;
