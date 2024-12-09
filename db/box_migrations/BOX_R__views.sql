drop view if exists v_field;

create view v_field
as
SELECT fi.type,
       fi.name,
       fi.widget,
       fi.foreign_entity,
       fi.foreign_value_field,
       fi."lookupQuery",
       fi.local_key_columns,
       fi.foreign_key_columns,
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




create or replace function v_field_ins() returns trigger
    set search_path from current
    language plpgsql
as
$$
begin
    insert into field (type, name, widget, foreign_entity,foreign_value_field, "lookupQuery", local_key_columns, foreign_key_columns, "childQuery", "default", "conditionFieldId", "conditionValues", params, read_only, required, form_uuid, child_form_uuid, function,min,max,roles) values
        (new.type, new.name, new.widget, new.foreign_entity, new.foreign_value_field, new."lookupQuery", new.local_key_columns, new.foreign_key_columns, new."childQuery", new."default", new."conditionFieldId", new."conditionValues", new.params, new.read_only, new.required, new.form_uuid, new.child_form_uuid, new.function,new.min,new.max, new.roles)
    returning field_uuid into new.field_uuid;

    select count(*)>0 into new.entity_field from information_schema.columns where table_name=(select entity from form where form_uuid=new.form_uuid) and column_name=new.name;

    return new;
end;
$$;






create or replace function v_field_upd() returns trigger
    set search_path from current
    language plpgsql
as
$$
begin
    update field
    set type = new.type,
        name = new.name,
        widget = new.widget,
        foreign_entity = new.foreign_entity,
        foreign_value_field = new.foreign_value_field,
        "lookupQuery" = new."lookupQuery",
        local_key_columns = new.local_key_columns,
        foreign_key_columns = new.foreign_key_columns,
        "childQuery" = new."childQuery",
        "default" = new."default",
        "conditionFieldId" = new."conditionFieldId",
        "conditionValues" = new."conditionValues",
        params = new.params,
        read_only = new.read_only,
        required = new.required,
        form_uuid = new.form_uuid,
        child_form_uuid = new.child_form_uuid,
        function = new.function,
        min = new.min,
        max = new.max,
        roles = new.roles
    where field_uuid = new.field_uuid;

    select count(*)>0 into new.entity_field from information_schema.columns where table_name=(select entity from form where form_uuid=new.form_uuid) and column_name=new.name;

    return new;
end;
$$;





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

