
alter table mails add column reply_to email;

create function mail_notification(_mail_from email, _mail_reply_to email, _mail_to email[], _mail_cc email[], _mail_bcc email[], _subject text, _text text, _html text, _params jsonb) returns uuid
    SET search_path from current
    language plpgsql
as
$$
declare
    _mail_id uuid;
begin

    insert into mails (send_at,wished_send_at,mail_from,mail_to,mail_cc,mail_bcc,subject,text,html,params,created,reply_to) values
        (now(),now(),_mail_from, _mail_to, _mail_cc,_mail_bcc, _subject, _text, _html,_params, now(),_mail_reply_to) returning id into _mail_id;

    -- subtracting the amount from the sender's account
    PERFORM pg_notify('mail_feedback_channel','{"sendMail": true}');

    return _mail_id;
end;
$$;

alter function mail_notification(email, email, email[], email[], email[], text, text, text, jsonb) owner to postgres;

create or replace function mail_notification(_mail_from email, _mail_to email[], _mail_cc email[], _mail_bcc email[], _subject text, _text text, _html text, _params jsonb) returns uuid
    SET search_path from current
    language sql
as
$$

    select mail_notification(_mail_from, null::email, _mail_to, _mail_cc, _mail_bcc, _subject, _text, _html, _params);

$$;

alter function mail_notification(email, email[], email[], email[], text, text, text, jsonb) owner to postgres;




