drop view box.v_field;
create view box.v_field
            (type, name, widget, "lookupEntity", "lookupValueField", "lookupQuery", "masterFields", "childFields",
             "childQuery", "default", "conditionFieldId", "conditionValues", params, read_only, required, field_uuid,
             form_uuid, child_form_uuid, function, min, max, entity_field)
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
       (SELECT count(*) > 0
        FROM information_schema.columns
        WHERE columns.table_name::name = f.entity::text
          AND columns.column_name::name = fi.name::text) AS entity_field
FROM box.field fi
         LEFT JOIN box.form f ON fi.form_uuid = f.form_uuid;

alter table box.v_field
    owner to postgres;

create trigger v_field_del
    instead of delete
    on box.v_field
    for each row
execute procedure box.v_field_del();


create or replace function box.v_field_ins() returns trigger
    language plpgsql
as
$$
begin
    insert into box.field (type, name, widget, "lookupEntity", "lookupValueField", "lookupQuery", "masterFields", "childFields", "childQuery", "default", "conditionFieldId", "conditionValues", params, read_only, required, form_uuid, child_form_uuid, function,min,max) values
        (new.type, new.name, new.widget, new."lookupEntity", new."lookupValueField", new."lookupQuery", new."masterFields", new."childFields", new."childQuery", new."default", new."conditionFieldId", new."conditionValues", new.params, new.read_only, new.required, new.form_uuid, new.child_form_uuid, new.function,new.min,new.max)
    returning field_uuid into new.field_uuid;

    select count(*)>0 into new.entity_field from information_schema.columns where table_name=(select entity from box.form where form_uuid=new.form_uuid) and column_name=new.name;

    return new;
end;
$$;

alter function box.v_field_ins() owner to postgres;

grant execute on function box.v_field_ins() to wsb;

grant execute on function box.v_field_ins() to bafu;



create trigger v_field_ins
    instead of insert
    on box.v_field
    for each row
execute procedure box.v_field_ins();


create or replace function box.v_field_upd() returns trigger
    language plpgsql
as
$$
begin
    update box.field
    set type = new.type,
        name = new.name,
        widget = new.widget,
        "lookupEntity" = new."lookupEntity",
        "lookupValueField" = new."lookupValueField",
        "lookupQuery" = new."lookupQuery",
        "masterFields" = new."masterFields",
        "childFields" = new."childFields",
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
        max = new.max
    where field_uuid = new.field_uuid;

    select count(*)>0 into new.entity_field from information_schema.columns where table_name=(select entity from box.form where form_uuid=new.form_uuid) and column_name=new.name;

    return new;
end;
$$;

alter function box.v_field_upd() owner to postgres;

grant execute on function box.v_field_upd() to wsb;

grant execute on function box.v_field_upd() to bafu;



create trigger v_field_upd
    instead of update
    on box.v_field
    for each row
execute procedure box.v_field_upd();

