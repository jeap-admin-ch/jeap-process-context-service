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

ALTER TABLE process_instance_process_data
    ADD CONSTRAINT uc_process_data UNIQUE NULLS NOT DISTINCT (process_instance_id, key_, value_, role);
