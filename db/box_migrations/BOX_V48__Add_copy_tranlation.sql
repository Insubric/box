
ALTER TABLE field_i18n DROP CONSTRAINT IF EXISTS field_i18n_label_lang_key;
alter table field_i18n add constraint field_i18n_label_lang_key unique (lang,field_uuid);

ALTER TABLE form_i18n DROP CONSTRAINT IF EXISTS form_i18n_label_lang_key;
alter table form_i18n add constraint form_i18n_label_lang_key unique (lang,form_uuid);

ALTER TABLE function_i18n DROP CONSTRAINT IF EXISTS function_i18n_label_lang_key;
alter table function_i18n add constraint function_i18n_label_lang_key unique (lang,function_uuid);

ALTER TABLE function_field_i18n DROP CONSTRAINT IF EXISTS function_field_i18n_label_lang_key;
alter table function_field_i18n add constraint function_field_i18n_label_lang_key unique (lang,field_uuid);

ALTER TABLE export_i18n DROP CONSTRAINT IF EXISTS export_i18n_label_lang_key;
alter table export_i18n add constraint export_i18n_label_lang_key unique (lang,export_uuid);

ALTER TABLE export_field_i18n DROP CONSTRAINT IF EXISTS export_field_i18n_label_lang_key;
alter table export_field_i18n add constraint export_field_i18n_label_lang_key unique (lang,field_uuid);

create or replace function translation_copy_lang(from_lang text, to_lang text) returns void language sql SET search_path=box as $$

    insert into field_i18n (lang, label, placeholder, tooltip, hint, "lookupTextField", field_uuid)
    select to_lang,label, placeholder, tooltip, hint, "lookupTextField", field_uuid from field_i18n where lang=from_lang
    on conflict do nothing;

    insert into form_i18n (lang, label, view_table, dynamic_label, form_uuid)
    select to_lang,label, view_table, dynamic_label, form_uuid from form_i18n where lang=from_lang
    on conflict do nothing;

    insert into function_i18n (lang, label, tooltip, hint, function, function_uuid)
    select to_lang,label, tooltip, hint, function, function_uuid from function_i18n where lang=from_lang
    on conflict do nothing;

    insert into function_field_i18n (lang, label, placeholder, tooltip, hint, "lookupTextField", field_uuid)
    select to_lang, label, placeholder, tooltip, hint, "lookupTextField", field_uuid from function_field_i18n where lang=from_lang
    on conflict do nothing;

    insert into export_i18n (lang, label, tooltip, hint, function, export_uuid)
    select to_lang, label, tooltip, hint, function, export_uuid from export_i18n where lang=from_lang
    on conflict do nothing;

    insert into export_field_i18n (lang, label, placeholder, tooltip, hint, "lookupTextField", field_uuid)
    select to_lang, label, placeholder, tooltip, hint, "lookupTextField", field_uuid from export_field_i18n where lang=from_lang
    on conflict do nothing;

    insert into labels (lang, key, label)
    select to_lang,key,label from labels where lang=from_lang
    on conflict do nothing;

$$;

