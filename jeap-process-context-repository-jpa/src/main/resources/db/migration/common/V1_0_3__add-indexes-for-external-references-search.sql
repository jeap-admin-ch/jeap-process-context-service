CREATE INDEX process_instance_template_name ON process_instance (template_name);
CREATE INDEX external_references_name_value ON process_instance_external_references (name, value);


