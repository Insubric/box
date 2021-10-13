alter table box.field drop column if exists linked_label_fields;
alter table box.field drop column if exists linked_key_fields;
alter table box.field_i18n add column if not exists static_content text;