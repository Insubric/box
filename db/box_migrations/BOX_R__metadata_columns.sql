create or replace view metadata_columns as
SELECT att.attname                              as column_name,
       format_type(att.atttypid, att.atttypmod) as data_type,
       not att.attnotnull                       as nullable,
       mv.relname                               as table_name,
       nsp.nspname                              as table_schema,
       att.attnum                               AS ordinal_position,
       pg_get_expr(d.adbin, d.adrelid)          AS column_default,
       case  when mv.relkind = 'r' then 'table'
             when mv.relkind = 'v' then 'view'
             when mv.relkind = 'f' then 'foreign_table'
             when mv.relkind = 'm' then 'materialized_view'
             else 'other'
           end as table_type
from pg_catalog.pg_attribute att
         LEFT JOIN pg_catalog.pg_attrdef d ON (att.attrelid, att.attnum) = (d.adrelid, d.adnum)
         join pg_catalog.pg_class mv ON mv.oid = att.attrelid
         join pg_catalog.pg_namespace nsp ON nsp.oid = mv.relnamespace
where mv.relkind in ('r', 'v', 'm', 'f') and
    not att.attisdropped
  and att.attnum > 0
order by att.attnum;