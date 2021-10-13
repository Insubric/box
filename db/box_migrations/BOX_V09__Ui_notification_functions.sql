create or replace function box.ui_notification(
    topic text,
    users text[],
    payload json
)
returns void as $$
    declare
        output text := '';
    begin

        output := row_to_json(
                (SELECT ColumnName FROM (SELECT topic,users,payload) AS ColumnName (topic,allowed_users,payload))
            )::text;

        -- subtracting the amount from the sender's account
        PERFORM pg_notify('ui_feedback_channel',output);
    end;
$$ LANGUAGE plpgsql;

create or replace function box.ui_notification_forall(
    topic text,
    payload json
)
    returns void as $$
begin
    PERFORM box.ui_notification(topic,'{"ALL_USERS"}'::text[],payload);
end;
$$ LANGUAGE plpgsql;