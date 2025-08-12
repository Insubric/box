create or replace function mail_notification(
    mail_from text,
    mail_to text[],
    subject text,
    text text,
    html text
)
    returns void as $$
declare
    output text := '';
begin

    output := row_to_json(
            (SELECT ColumnName FROM (SELECT mail_from,mail_to,subject,text,html) AS ColumnName ("from","to",subject,text,html))
        )::text;

    -- subtracting the amount from the sender's account
    PERFORM pg_notify('mail_feedback_channel',output);
end;
$$ LANGUAGE plpgsql;