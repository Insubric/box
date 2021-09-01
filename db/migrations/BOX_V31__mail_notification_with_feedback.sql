alter table box.mails add column if not exists wished_send_at timestamp;
update box.mails m
set wished_send_at=m2.created
from (select id,created from box.mails) as m2
where m.id = m2.id;
alter table box.mails alter column wished_send_at set not null;

drop function if exists box.mail_notification(_mail_from text, _mail_to text[], _subject text, _text text, _html text);
drop function if exists box.mail_notification(_mail_from text, _mail_to text[], _subject text, _text text, _html text, _params jsonb);
commit;



create function box.mail_notification(_mail_from text, _mail_to text[], _subject text, _text text, _html text, _params jsonb) returns uuid
    language plpgsql
as
$$
declare
    _mail_id uuid;
begin

    insert into box.mails (send_at,wished_send_at,mail_from,mail_to,subject,text,html,params,created) values
    (now(),now(),_mail_from, _mail_to, _subject, _text, _html,_params, now()) returning id into _mail_id;

    -- subtracting the amount from the sender's account
    PERFORM pg_notify('mail_feedback_channel','{"sendMail": true}');

    return _mail_id;
end;
$$;

create function box.mail_notification(_mail_from text, _mail_to text[], _subject text, _text text, _html text) returns uuid
    language sql as
$$
select box.mail_notification(_mail_from,_mail_to,_subject,_text,_html,'{}'::jsonb);
$$;


create or replace function box.check_mail_sent(_mail_id uuid) returns boolean
    language sql as
$$
    select coalesce((select true from box.mails where id=_mail_id and sent_at is not null),false);
$$;
