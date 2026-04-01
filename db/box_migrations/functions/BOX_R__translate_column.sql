create or replace function translate_column(schema text, _table text, from_lang text, to_lang text, from_column text, to_column text) returns boolean
    SET search_path from current
    language plpgsql as $$
begin
    perform box_notify('translate_column',
                          jsonb_build_object(
                                  'schema',schema,
                                  'table', _table,
                                  'from_lang', from_lang,
                                  'to_lang',to_lang,
                                  'from_column',  from_column,
                                  'to_column',to_column
                          ));
    return true;
end;
$$;