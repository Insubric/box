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
       fi.condition,
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
    insert into field (type, name, widget, foreign_entity,foreign_value_field, "lookupQuery", local_key_columns, foreign_key_columns, "childQuery", "default", condition, params, read_only, required, form_uuid, child_form_uuid, function,min,max,roles) values
        (new.type, new.name, new.widget, new.foreign_entity, new.foreign_value_field, new."lookupQuery", new.local_key_columns, new.foreign_key_columns, new."childQuery", new."default", new.condition, new.params, new.read_only, new.required, new.form_uuid, new.child_form_uuid, new.function,new.min,new.max, new.roles)
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
        condition = new.condition,
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



drop view if exists v_roles;
create or replace view v_roles
as

with recursive roles as (
    SELECT r.rolname,b.rolname as parent,greatest(coalesce(u.access_level_id,-1),coalesce(u2.access_level_id,-1)) as access_level_id
    FROM pg_roles r
             join pg_auth_members m on m.member = r.oid
             JOIN pg_roles b ON m.roleid = b.oid
             left join users u on u.username = r.rolname
             left join users u2 on u2.username = b.rolname
    WHERE r.rolname !~ '^pg_'::text
    union distinct

    select r.rolname,b.rolname as parent,greatest(r.access_level_id,coalesce(u.access_level_id,-1)) as access_level_id from roles r
                                                  join pg_auth_members m on m.member = (select oid from pg_roles where rolname = r.parent)
                                                  JOIN pg_roles b ON m.roleid = b.oid
                                                  left join users u on u.username = b.rolname

), parent_roles as (select rolname, array_agg(parent) as parents,max(access_level_id) as access_level_id
                    from roles
                    group by rolname
)
SELECT r.rolname,
       r.rolsuper,
       r.rolinherit,
       r.rolcreaterole,
       r.rolcreatedb,
       r.rolcanlogin,
       r.rolconnlimit,
       r.rolvaliduntil,
       coalesce(pr.parents,array[]::text[]) AS memberof,
       r.rolreplication,
       r.rolbypassrls,
       coalesce(greatest(pr.access_level_id,u.access_level_id),-1) as access_level_id
FROM pg_roles r
         left join parent_roles pr on r.rolname = pr.rolname
         left join users u on u.username = r.rolname
WHERE r.rolname !~ '^pg_'::text
ORDER BY r.rolname;

create or replace view v_foreign_keys as
select
    c.conname as constraint_name,
    s.nspname as schema_name,
    t.relname as table_name,
    s2.nspname as referenced_schema_name,
    t2.relname as referenced_table_name,
    (
        select ARRAY_AGG(a.attname ORDER BY t.ordinality)
        from pg_attribute a,unnest(c.conkey)  WITH ORDINALITY AS t(attnum, ordinality)
        where a.attrelid = c.conrelid AND a.attnum = t.attnum
    )  AS columns,
    (
        select ARRAY_AGG(a.attname ORDER BY t.ordinality)
        from pg_attribute a,unnest(c.confkey) WITH ORDINALITY AS t(attnum, ordinality)
        where a.attrelid = c.confrelid  AND a.attnum = t.attnum
    )  AS referenced_columns
from pg_constraint c
         JOIN pg_class t ON t.oid = c.conrelid
         left JOIN pg_namespace s ON t.relnamespace = s.oid
         JOIN pg_class t2 ON t2.oid = c.confrelid
         left JOIN pg_namespace s2 ON t2.relnamespace = s2.oid
where c.contype='f';

grant select on v_foreign_keys to box_user;