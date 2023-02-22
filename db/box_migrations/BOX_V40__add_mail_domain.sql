
CREATE EXTENSION if not exists citext;

--create types
DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'email') THEN
            CREATE DOMAIN email AS citext
                CHECK ( value ~ '^[a-zA-Z0-9.!#$%&''*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$' );
        END IF;
        --more types here...
    END$$;



drop function if exists mail_notification(mail_from text, mail_to text[], text text, html text);
drop function if exists mail_notification(_mail_from text, _mail_to text[], _subject text, _text text, _html text);
drop function if exists mail_notification(_mail_from text, _mail_to text[], _subject text, _text text, _html text, _params jsonb);

create or replace function mail_notification(mail_from email, mail_to email[], text text, html text) returns void
    language plpgsql
as
$$
declare
    output text := '';
begin

    output := row_to_json(
            (SELECT ColumnName FROM (SELECT mail_from,mail_to,text,html) AS ColumnName ("from","to",text,html))
        )::text;

    -- subtracting the amount from the sender's account
    PERFORM pg_notify('mail_feedback_channel',output);
end;
$$;

alter function mail_notification(email, email[], text, text) owner to postgres;


create or replace function mail_notification(_mail_from email, _mail_to email[], _subject text, _text text, _html text, _params jsonb) returns uuid
    language plpgsql
as
$$
declare
    _mail_id uuid;
begin

    insert into mails (send_at,wished_send_at,mail_from,mail_to,subject,text,html,params,created) values
        (now(),now(),_mail_from, _mail_to, _subject, _text, _html,_params, now()) returning id into _mail_id;

    -- subtracting the amount from the sender's account
    PERFORM pg_notify('mail_feedback_channel','{"sendMail": true}');

    return _mail_id;
end;
$$;

alter function mail_notification(email, email[], text, text, text, jsonb) owner to postgres;


create or replace function mail_notification(_mail_from email, _mail_to email[], _subject text, _text text, _html text) returns uuid
    language sql
as
$$
select mail_notification(_mail_from,_mail_to,_subject,_text,_html,'{}'::jsonb);
$$;

alter function mail_notification(email, email[], text, text, text) owner to postgres;



create or replace function mail_notification(_mail_from email, _mail_to email[], _subject text, _text text, _html text) returns uuid
    language sql
as
$$
select mail_notification(_mail_from,_mail_to,_subject,_text,_html,'{}'::jsonb);
$$;

alter function mail_notification(email, email[], text, text, text) owner to postgres;

