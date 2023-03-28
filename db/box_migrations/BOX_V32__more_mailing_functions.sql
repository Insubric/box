create or replace function mail_sent_at(_mail_id uuid) returns timestamp
    language sql as
$$
select sent_at from mails where id=_mail_id;
$$;

create or replace function check_mail_sent(_mail_id uuid) returns boolean
    language sql security definer as
$$
select coalesce((select true from mails where id=_mail_id and sent_at is not null),false);
$$;
