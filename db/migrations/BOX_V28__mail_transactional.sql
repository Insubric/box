
drop table if exists box.mails;
create table if not exists box.mails(
    id uuid not null default gen_random_uuid() primary key,
    send_at timestamp not null,
    sent_at timestamp,
    mail_from text not null,
    mail_to text[] not null,
    subject text not null,
    html text not null,
    text text,
    params jsonb,
    created timestamp not null
);

drop function if exists box.mail_notification(mail_from text, mail_to text[], subject text, text text, html text);
create or replace function box.mail_notification(
    _mail_from text,
    _mail_to text[],
    _subject text,
    _text text,
    _html text
)
    returns void as $$
declare
    output text := '';
begin

    insert into box.mails (send_at,mail_from,mail_to,subject,text,html,created) values
    (now(), _mail_from, _mail_to, _subject, _text, _html, now());

    output :=  jsonb_build_object('send_at',now())::text;

    -- subtracting the amount from the sender's account
    PERFORM pg_notify('mail_feedback_channel','{"sendMail": true}');
end;
$$ LANGUAGE plpgsql;

create or replace function box.mail_notification(
    _mail_from text,
    _mail_to text[],
    _subject text,
    _text text,
    _html text,
    _params jsonb
)
    returns void as $$
declare
    output text := '';
begin

    insert into box.mails (send_at,mail_from,mail_to,subject,text,html,params,created) values
    (now(), _mail_from, _mail_to, _subject, _text, _html,_params, now());

    output :=  jsonb_build_object('send_at',now())::text;

    -- subtracting the amount from the sender's account
    PERFORM pg_notify('mail_feedback_channel','{"sendMail": true}');
end;
$$ LANGUAGE plpgsql;