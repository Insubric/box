alter table mails add column if not exists wished_send_at timestamp;
update mails m
set wished_send_at=m2.created
from (select id,created from mails) as m2
where m.id = m2.id;
alter table mails alter column wished_send_at set not null;

drop function if exists mail_notification(_mail_from text, _mail_to text[], _subject text, _text text, _html text);
drop function if exists mail_notification(_mail_from text, _mail_to text[], _subject text, _text text, _html text, _params jsonb);
commit;



create function mail_notification(_mail_from text, _mail_to text[], _subject text, _text text, _html text, _params jsonb) returns uuid
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

create function mail_notification(_mail_from text, _mail_to text[], _subject text, _text text, _html text) returns uuid
    language sql as
$$
select mail_notification(_mail_from,_mail_to,_subject,_text,_html,'{}'::jsonb);
$$;


create or replace function check_mail_sent(_mail_id uuid) returns boolean
    language sql as
$$
    select coalesce((select true from mails where id=_mail_id and sent_at is not null),false);
$$;
