create or replace function ui_notification(topic text, users text[], payload json) returns void
    language plpgsql
    set search_path from current
as
$$
declare
    output text := '';
begin

    output := row_to_jsonb(
            (SELECT ColumnName FROM (SELECT topic,users,payload) AS ColumnName (topic,allowed_users,payload))
              )::text;

    -- subtracting the amount from the sender's account
    PERFORM box_notify('ui_feedback_channel',output);
end;
$$;


create or replace function ui_notification_forall(topic text, payload json) returns void
    language plpgsql
    set search_path from current
as
$$
begin
    PERFORM ui_notification(topic,'{"ALL_USERS"}'::text[],payload);
end;
$$;