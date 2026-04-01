drop view if exists v_jsonschema;
create or replace view v_jsonschema as
WITH cols AS not materialized (
    SELECT
        column_name,
        data_type,
        is_nullable,
        character_maximum_length,
        numeric_precision,
        numeric_scale,
        column_default,
        table_schema,
        table_name,
        jsonb_strip_nulls(jsonb_build_object(
                'type', CASE
                            WHEN data_type IN ('integer','bigint','smallint') THEN '"integer"'::jsonb
                            WHEN data_type IN ('numeric','decimal','real','double precision') THEN '"number"'::jsonb
                            WHEN data_type = 'boolean' THEN '"boolean"'::jsonb
                            WHEN data_type LIKE '%char%' OR data_type = 'text' THEN '"string"'::jsonb
                            WHEN data_type = 'date' THEN '"string"'::jsonb
                            WHEN data_type LIKE 'timestamp%' THEN '"string"'::jsonb
                            WHEN data_type IN ('json','jsonb') THEN '"object"'::jsonb
                            ELSE '"string"'::jsonb END,
                'format', case
                              WHEN data_type = 'date' THEN '"date"'::jsonb
                              WHEN data_type LIKE 'timestamp%' THEN '"date-time"'::jsonb
                              else null::jsonb end,
                'nullable', (is_nullable = 'YES'),
                'maxLength', CASE
                                 WHEN character_maximum_length IS NOT NULL THEN to_jsonb(character_maximum_length)
                                 ELSE NULL
                    END,
                'precision', CASE
                                 WHEN numeric_precision IS NOT NULL THEN to_jsonb(numeric_precision)
                                 ELSE NULL
                    END,
                'scale', CASE
                             WHEN numeric_scale IS NOT NULL THEN to_jsonb(numeric_scale)
                             ELSE NULL
                    END,
                'default', to_jsonb(column_default)
                          )) as column_jsonschema
    FROM information_schema.columns
)
SELECT table_schema::text,table_name::text,jsonb_build_object(
        '$schema', 'https://json-schema.org/draft/2020-12/schema',
        '$id',     to_jsonb('urn:postgres:' || table_schema || ':' || table_name),
        'title',   to_jsonb(table_name),
        'type',    'object',
        'properties', jsonb_object_agg(column_name,column_jsonschema),
        'required', (SELECT jsonb_agg(column_name)
                     FROM cols c
                     WHERE c.is_nullable = 'NO' and c.table_schema = cols.table_schema and c.table_name=cols.table_name)
                               )::jsonb AS json_schema
FROM cols
group by table_schema, table_name;

grant select on v_jsonschema to box_user;
