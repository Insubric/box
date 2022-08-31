alter domain box.email drop constraint email_check;
alter domain box.email add
    constraint email_check check (VALUE OPERATOR (box.~)
                                  '^([a-zA-Z0-9.!#$%&''*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*)?(.+ <[a-zA-Z0-9.!#$%&''*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*>)?$'::box.citext);


alter table box.mails add column mail_cc text[] not null default array[]::text[];
alter table box.mails add column mail_bcc text[] not null default array[]::text[];

drop function if exists box.mail_notification(mail_from box.email, mail_to box.email[], text text, html text);



create function box.mail_notification(_mail_from box.email, _mail_to box.email[], _mail_cc box.email[], _mail_bcc box.email[], _subject text, _text text, _html text, _params jsonb) returns uuid
    language plpgsql
as
$$
declare
    _mail_id uuid;
begin

    insert into box.mails (send_at,wished_send_at,mail_from,mail_to,mail_cc,mail_bcc,subject,text,html,params,created) values
        (now(),now(),_mail_from, _mail_to, _mail_cc,_mail_bcc, _subject, _text, _html,_params, now()) returning id into _mail_id;

    -- subtracting the amount from the sender's account
    PERFORM pg_notify('mail_feedback_channel','{"sendMail": true}');

    return _mail_id;
end;
$$;

create or replace function box.mail_notification(_mail_from box.email, _mail_to box.email[], _subject text, _text text, _html text, _params jsonb) returns uuid
    language sql
as
$$
select box.mail_notification(_mail_from,_mail_to,array[]::box.email[],array[]::box.email[],_subject,_text,_html,_params);
$$;
