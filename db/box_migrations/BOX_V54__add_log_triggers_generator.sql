
do
$do$
    begin
        if exists (select from pg_roles where rolname = 'box_user') then
            raise notice 'skip box_user creation';
        else
            create role box_user;
        end if;

        execute format('grant usage on schema %I to box_user', current_schema);
    end
$do$;


create or replace function tg_log() returns trigger
    SET search_path from current
    security definer
    language plpgsql
as
$$
begin
    if(tg_op = 'DELETE') then
        execute format('insert into %I.%I select ''' || tg_op || ''',now(),current_user, to_jsonb($1)',tg_table_schema || '_log',tg_table_name) using old;
    elseif(old <> new) then
        execute format('insert into %I.%I select ''' || tg_op || ''',now(),current_user, to_jsonb($1)',tg_table_schema || '_log',tg_table_name) using old;
    end if;
    return null;
end
$$;


grant execute on function tg_log() to box_user;

create or replace function add_log(schema text, _table text) returns text
    SET search_path from current
    language plpgsql as $$
begin
    execute  format('create schema if not exists %I',schema || '_log');
    execute  format('create table if not exists %I.%I (operation text, stamp timestamp, username text, data jsonb)',schema || '_log',_table);
    execute  format('create trigger logging after update or delete on %I.%I for each row execute procedure tg_log()',schema,_table);
    return format('Log installed successfully on %I',_table);
end
$$;

create or replace function add_log_for_schema(schema text, exclude text[]) returns text
    SET search_path from current
    language sql as $$

with tables as (
    select add_log(schema,table_name) as t from information_schema.tables
    where table_schema=schema and table_type='BASE TABLE' and not
        (
                    table_name = any (exclude) or
                    table_name in (select table_name from information_schema.tables where table_schema=schema || '_log' and table_type='BASE TABLE')
            )
) select string_agg(t,'
') from tables;


$$;

create or replace function add_log_for_schema(schema text) returns text
    SET search_path from current
    language sql as $$
select add_log_for_schema(schema,array['spatial_ref_sys','flyway_schema_history']::text[])
$$;

