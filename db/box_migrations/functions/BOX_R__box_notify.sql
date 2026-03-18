create or replace function box_notify(
    channel text,
    payload jsonb
)
    returns void set search_path from current as $$
declare
    output text := '';
begin

    output := to_jsonb(
            (SELECT t FROM (SELECT channel,payload) AS t (channel,payload))
    )::text;

    -- subtracting the amount from the sender's account
    PERFORM pg_notify('box_' || current_schema || '_channel',output);
end;
$$ LANGUAGE plpgsql;

