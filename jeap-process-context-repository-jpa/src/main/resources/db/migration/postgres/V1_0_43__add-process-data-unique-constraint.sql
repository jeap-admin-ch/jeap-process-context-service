DELETE
FROM process_instance_process_data
WHERE id IN (SELECT id
             FROM (SELECT id,
                          ROW_NUMBER() OVER (
                              PARTITION BY process_instance_id, key_, value_, role
                              ORDER BY created_at
                              ) as rn
                   FROM process_instance_process_data) dupes
             WHERE rn > 1);

CREATE UNIQUE INDEX CONCURRENTLY idx_process_data_unique
    ON process_instance_process_data
        (process_instance_id, key_, value_, role)
    NULLS NOT DISTINCT;
