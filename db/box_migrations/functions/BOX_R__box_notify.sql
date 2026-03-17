create or replace function box_notify(
    channel text,
    payload json
)
    returns void set search_path from current as $$
declare
    output text := '';
begin

    output := row_to_json(
            (SELECT ColumnName FROM (SELECT channel,payload) AS ColumnName (channel,payload))
              )::text;

    -- subtracting the amount from the sender's account
    PERFORM pg_notify('box_' || current_schema || '_channel',output);
end;
$$ LANGUAGE plpgsql;

