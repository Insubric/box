alter domain email drop constraint email_check;
alter domain email add
    constraint email_check check (VALUE OPERATOR (~)
                                  '^([a-zA-Z0-9.!#$%&''*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*)?(.+ <[a-zA-Z0-9.!#$%&''*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*>)?$'::citext);


alter table mails add column mail_cc text[] not null default array[]::text[];
alter table mails add column mail_bcc text[] not null default array[]::text[];

drop function if exists mail_notification(mail_from email, mail_to email[], text text, html text);



create function mail_notification(_mail_from email, _mail_to email[], _mail_cc email[], _mail_bcc email[], _subject text, _text text, _html text, _params jsonb) returns uuid
    language plpgsql
as
$$
declare
    _mail_id uuid;
begin

    insert into mails (send_at,wished_send_at,mail_from,mail_to,mail_cc,mail_bcc,subject,text,html,params,created) values
        (now(),now(),_mail_from, _mail_to, _mail_cc,_mail_bcc, _subject, _text, _html,_params, now()) returning id into _mail_id;

    -- subtracting the amount from the sender's account
    PERFORM pg_notify('mail_feedback_channel','{"sendMail": true}');

    return _mail_id;
end;
$$;

create or replace function mail_notification(_mail_from email, _mail_to email[], _subject text, _text text, _html text, _params jsonb) returns uuid
    language sql
as
$$
select mail_notification(_mail_from,_mail_to,array[]::email[],array[]::email[],_subject,_text,_html,_params);
$$;
