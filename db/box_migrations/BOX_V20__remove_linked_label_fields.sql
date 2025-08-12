alter table field drop column if exists linked_label_fields;
alter table field drop column if exists linked_key_fields;
alter table field_i18n add column if not exists static_content text;