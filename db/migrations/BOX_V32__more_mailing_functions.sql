create or replace function box.mail_sent_at(_mail_id uuid) returns timestamp
    language sql as
$$
select sent_at from box.mails where id=_mail_id;
$$;

create or replace function box.check_mail_sent(_mail_id uuid) returns boolean
    language sql security definer as
$$
select coalesce((select true from box.mails where id=_mail_id and sent_at is not null),false);
$$;
