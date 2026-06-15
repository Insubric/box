
CREATE OR REPLACE FUNCTION select_table_data(
    schema text, entity text, fields text)
    RETURNS SETOF RECORD
    security invoker
    LANGUAGE 'plpgsql'
AS $BODY$
BEGIN
    RETURN QUERY EXECUTE format('select to_jsonb(t) FROM (select %s from %I.%I) t ',fields,schema,entity);
END
$BODY$;
drop view if exists v_lookup_data;
create view v_lookup_data with ( security_invoker = true) as
with all_foreign_fields as (select foreign_entity entity, foreign_value_field field
                            from field
                            where foreign_entity is not null
                            union all
                            select foreign_entity, fkc
                            from field,
                                 unnest(foreign_key_columns) fkc
                            where foreign_entity is not null
                            union all
                            select foreign_entity, unnest(fi.foreign_label_columns)
                            from field f
                                     join field_i18n fi using (field_uuid)
                            where foreign_entity is not null),
     agg as (select entity,

                    array_agg(distinct field) fields,
                    '"' || string_agg(distinct field,'","') || '"' fields_txt
             from all_foreign_fields aff
                      join metadata_columns mc on aff.entity = mc.table_name and aff.field = mc.column_name
             group by entity
     )
select entity,fields,jsonb_agg(data) data from agg,select_table_data(coalesce((select value from conf where key='default-schema'),'public'),entity,fields_txt) d(data jsonb)
group by entity,fields;

grant select on v_lookup_data to box_user;