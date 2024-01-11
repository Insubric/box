

create or replace view v_box_usages as
with box_usages as (select name, entity as entity
                    from form
                    union
                    distinct
                    select name, view_table as entity
                    from form
                    union
                    distinct
                    select form.name, form_i18n.view_table as name
                    from form_i18n
                             left join form on form_i18n.form_uuid = form.form_uuid
                    union
                    distinct
                    select f.name, "lookupEntity" as name
                    from field
                             left join form f on field.form_uuid = f.form_uuid
)
select * from box_usages where entity is not null;

create or replace function form_usages(form_name text) returns table(root text, guest_user text, childs jsonb, child_list text[], index_page boolean)
    language sql
    stable
    set search_path from current as
$$
with recursive root_forms as (
    select
        form.form_uuid as root_uuid,
        form.form_uuid as form_uuid,
        null::uuid as parent_uuid
    from form
             left join field on form.form_uuid = field.child_form_uuid

    union all
    select
        rf.root_uuid,
        cf.form_uuid,
        rf.form_uuid
    from root_forms rf
             left join field rfields on rf.form_uuid=rfields.form_uuid and child_form_uuid is not null
             left join form cf on rfields.child_form_uuid = cf.form_uuid
    where cf.form_uuid is not null
), child_tree as (select r.name as root,r.guest_user, p.name, jsonb_build_object(coalesce(p.name,'null'),jsonb_agg(distinct f.name)) as childs, string_agg(distinct f.name,',') as childs_array
                  from root_forms rf
                           left join form r on r.form_uuid = rf.root_uuid
                           left join form f on f.form_uuid = rf.form_uuid
                           left join form p on p.form_uuid = rf.parent_uuid
                  group by r.name, p.name, r.guest_user
), all_childs as (
    select root, guest_user, jsonb_agg(childs) as childs, string_agg(childs_array,',') as childs_array
    from child_tree
    group by root, guest_user
),
               aggreate as (select root,
                                   guest_user,
                                   childs,
                                   string_to_array(childs_array, ',') child_list,
                                   (select count(*) > 0 from ui where key = 'index.page' and value = root) as index_page
                            from all_childs
               )
select * from aggreate where root=form_name or form_name = any(child_list);
$$;

create or replace view v_box_form_childs as
select name,entity,form_uuid,unnest(child_list) as child,index_page from  form f,form_usages(f.name);

-- https://stackoverflow.com/questions/12815496/export-specific-rows-from-a-postgresql-table-as-insert-sql-script/50510380#50510380
CREATE OR REPLACE FUNCTION dump(IN p_schema text, IN p_table text, IN p_where text)
    RETURNS setof text AS
$BODY$
DECLARE
    dumpquery_0 text;
    dumpquery_1 text;
    selquery text;
    selvalue text;
    valrec record;
    colrec record;
BEGIN

    -- ------ --
    -- GLOBAL --
    --   build base INSERT
    --   build SELECT array[ ... ]
    dumpquery_0 := 'INSERT INTO ' ||  quote_ident(p_schema) || '.' || quote_ident(p_table) || '(';
    selquery    := 'SELECT array[';

    <<label0>>
    FOR colrec IN SELECT table_schema, table_name, column_name, data_type
                  FROM information_schema.columns
                  WHERE table_name = p_table and table_schema = p_schema
                  ORDER BY ordinal_position
        LOOP
            dumpquery_0 := dumpquery_0 || quote_ident(colrec.column_name) || ',';
            selquery    := selquery    || 'CAST(' || quote_ident(colrec.column_name) || ' AS TEXT),';
        END LOOP label0;

    dumpquery_0 := substring(dumpquery_0 ,1,length(dumpquery_0)-1) || ')';
    dumpquery_0 := dumpquery_0 || ' VALUES (';
    selquery    := substring(selquery    ,1,length(selquery)-1)    || '] AS MYARRAY';
    selquery    := selquery    || ' FROM ' ||quote_ident(p_schema)||'.'||quote_ident(p_table);
    selquery    := selquery    || ' WHERE '||p_where;
    -- GLOBAL --
    -- ------ --

    -- ----------- --
    -- SELECT LOOP --
    --   execute SELECT built and loop on each row
    <<label1>>
    FOR valrec IN  EXECUTE  selquery
        LOOP
            dumpquery_1 := '';
            IF not found THEN
                EXIT ;
            END IF;

            -- ----------- --
            -- LOOP ARRAY (EACH FIELDS) --
            <<label2>>
            FOREACH selvalue in ARRAY valrec.MYARRAY
                LOOP
                    IF selvalue IS NULL
                    THEN selvalue := 'NULL';
                    ELSE selvalue := quote_literal(selvalue);
                    END IF;
                    dumpquery_1 := dumpquery_1 || selvalue || ',';
                END LOOP label2;
            dumpquery_1 := substring(dumpquery_1 ,1,length(dumpquery_1)-1) || ');';
            -- LOOP ARRAY (EACH FIELD) --
            -- ----------- --

            -- debug: RETURN NEXT dumpquery_0 || dumpquery_1 || ' --' || selquery;
            -- debug: RETURN NEXT selquery;
            RETURN NEXT dumpquery_0 || dumpquery_1;

        END LOOP label1 ;
    -- SELECT LOOP --
    -- ----------- --

    RETURN ;
END
$BODY$
    LANGUAGE plpgsql VOLATILE;

create or replace function dump_form_single(form_name text) returns table(statement_order int, dump text) language sql set search_path from current as $$
select 1,dump(current_schema,'form','name=''' || form_name ||  '''')
union
select 2,dump(current_schema,'form_actions','form_uuid='''|| form_uuid || '''') from form f
                                                                                         join form_actions using (form_uuid)
where f.name=form_name
union
select 3,dump(current_schema,'form_actions_table','form_uuid='''|| form_uuid || '''') from form f
                                                                                               join form_actions_table using (form_uuid)
where f.name=form_name
union
select 4,dump(current_schema,'form_actions_top_table','form_uuid='''|| form_uuid || '''') from form f
                                                                                                   join form_actions_top_table using (form_uuid)
where f.name=form_name
union
select 5,dump(current_schema,'form_i18n','form_uuid='''|| form_uuid || '''') from form f
                                                                                      join form_i18n using (form_uuid)
where f.name=form_name
union
select 6,dump(current_schema,'form_navigation_actions','form_uuid='''|| form_uuid || '''') from form f
                                                                                                    join form_navigation_actions using (form_uuid)
where f.name=form_name
union
select 7,dump(current_schema,'field','form_uuid='''|| form_uuid || '''') from form f
                                                                                  join field using (form_uuid)
where f.name=form_name
union
select 8,dump(current_schema,'field_i18n','field_uuid='''|| field_uuid || '''') from form f
                                                                                         join field using (form_uuid)
                                                                                         join field_i18n using (field_uuid)
where f.name=form_name
$$;





create or replace function dump_form(form_name text) returns table(dump text) language sql set search_path from current as $$
with statements as (
    select 1 as statement_order,'-- Dump form generated by dump_form(' || form_name || ') from ' || inet_server_addr() ||
                                ', port: ' || inet_server_port() || ' at date ' || now() as dump
    union
    select 2 as statement_order,'delete from ' || current_schema || '.form where form_uuid=''' || form_uuid || '''' as dump from form where name in ( select child from v_box_form_childs where name=form_name)
    union
    select 3 + statement_order as statement_order,dump
    from v_box_form_childs c, dump_form_single(c.child)
    where name = form_name
)
select string_agg(dump,'
' order by statement_order)  from statements
$$;
