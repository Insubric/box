drop view if exists v_field;

create view v_field
as
SELECT fi.type,
       fi.name,
       fi.widget,
       fi."lookupEntity",
       fi."lookupValueField",
       fi."lookupQuery",
       fi."masterFields",
       fi."childFields",
       fi."childQuery",
       fi."default",
       fi."conditionFieldId",
       fi."conditionValues",
       fi.params,
       fi.read_only,
       fi.required,
       fi.field_uuid,
       fi.form_uuid,
       fi.child_form_uuid,
       fi.function,
       fi.min,
       fi.max,
       fi.roles,
       (SELECT count(*) > 0
        FROM information_schema.columns
        WHERE columns.table_name::name = f.entity::text
          AND columns.column_name::name = fi.name::text) AS entity_field
FROM field fi
         LEFT JOIN form f ON fi.form_uuid = f.form_uuid;

alter table v_field
    owner to postgres;

create trigger v_field_del
    instead of delete
    on v_field
    for each row
execute procedure v_field_del();

create trigger v_field_ins
    instead of insert
    on v_field
    for each row
execute procedure v_field_ins();

create trigger v_field_upd
    instead of update
    on v_field
    for each row
execute procedure v_field_upd();

