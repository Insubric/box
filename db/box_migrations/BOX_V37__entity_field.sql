
create or replace view box.v_field as
select
    fi.type, fi.name, fi.widget, fi."lookupEntity", fi."lookupValueField", fi."lookupQuery", fi."masterFields", fi."childFields", fi."childQuery", fi."default", fi."conditionFieldId", fi."conditionValues", fi.params, fi.read_only, fi.required, fi.field_uuid, fi.form_uuid, fi.child_form_uuid, fi.function,
    (select count(*)>0 from information_schema.columns where table_name=f.entity and column_name=fi.name) as entity_field
from box.field fi
left join box.form f on fi.form_uuid = f.form_uuid;


create function box.v_field_upd() returns trigger language plpgsql as $$
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
            function = new.function
        where field_uuid = new.field_uuid;

    select count(*)>0 into new.entity_field from information_schema.columns where table_name=(select entity from box.form where form_uuid=new.form_uuid) and column_name=new.name;

    return new;
end;
$$;

create or replace function box.v_field_ins() returns trigger language plpgsql as $$
begin
    insert into box.field (type, name, widget, "lookupEntity", "lookupValueField", "lookupQuery", "masterFields", "childFields", "childQuery", "default", "conditionFieldId", "conditionValues", params, read_only, required, form_uuid, child_form_uuid, function) values
    (new.type, new.name, new.widget, new."lookupEntity", new."lookupValueField", new."lookupQuery", new."masterFields", new."childFields", new."childQuery", new."default", new."conditionFieldId", new."conditionValues", new.params, new.read_only, new.required, new.form_uuid, new.child_form_uuid, new.function)
    returning field_uuid into new.field_uuid;

    select count(*)>0 into new.entity_field from information_schema.columns where table_name=(select entity from box.form where form_uuid=new.form_uuid) and column_name=new.name;

    return new;
end;
$$;

create or replace function box.v_field_del() returns trigger language plpgsql as $$
begin
     delete from box.field where field_uuid = old.field_uuid;
     return old;
end;
$$;

create trigger v_field_del instead of delete on box.v_field for each row execute function box.v_field_del();
create trigger v_field_ins instead of insert on box.v_field for each row execute function box.v_field_ins();
create trigger v_field_upd instead of update on box.v_field for each row execute function box.v_field_upd();