insert into process_instance_process_data(process_instance_id, key, value, role, created_at)
    (select pier.process_instance_id, pier.name as key, pier.value, null as role, CURRENT_TIMESTAMP as created_at from process_instance_external_references pier
     where (select count(*) from process_instance_process_data pipd
            where pipd.process_instance_id = pier.process_instance_id and pipd.key = pier.name and pipd.value = pier.value) = 0);

drop table process_instance_external_references;
