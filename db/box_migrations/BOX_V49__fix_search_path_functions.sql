create or replace function hasrole(rol text) returns boolean
    language plpgsql
    set search_path from current 
as
$$
DECLARE
    roles text[];
BEGIN
    select memberof into roles from v_roles where lower(rolname) = lower(current_user);
    return rol = any(roles);
END
$$;


create or replace function hasrolein(rol text[]) returns boolean
    language plpgsql
    set search_path from current
as
$$
DECLARE
    roles text[];
BEGIN
    select memberof into roles from v_roles where lower(rolname) = lower(current_user);
    return rol && roles;   --intersection of the 2 arrays
END
$$;


create or replace function ui_notification(topic text, users text[], payload json) returns void
    language plpgsql
    set search_path from current
as
$$
declare
    output text := '';
begin

    output := row_to_json(
            (SELECT ColumnName FROM (SELECT topic,users,payload) AS ColumnName (topic,allowed_users,payload))
        )::text;

    -- subtracting the amount from the sender's account
    PERFORM pg_notify('ui_feedback_channel',output);
end;
$$;


create or replace function ui_notification_forall(topic text, payload json) returns void
    language plpgsql
    set search_path from current
as
$$
begin
    PERFORM ui_notification(topic,'{"ALL_USERS"}'::text[],payload);
end;
$$;


create or replace function check_mail_sent(_mail_id uuid) returns boolean
    security definer
    language sql
    set search_path from current
as
$$
select coalesce((select true from mails where id=_mail_id and sent_at is not null),false);
$$;


create or replace function mail_sent_at(_mail_id uuid) returns timestamp without time zone
    language sql
    set search_path from current
as
$$
select sent_at from mails where id=_mail_id;
$$;


create or replace function hasrole(rol text, _user text) returns boolean
    security definer
    language plpgsql
    set search_path from current
as
$$
DECLARE
    roles text[];
BEGIN
    select memberof into roles from v_roles where lower(rolname) = lower(_user);
    return rol = any(roles);
END
$$;


create or replace function v_field_upd() returns trigger
    language plpgsql
    set search_path from current
as
$$
begin
    update field
    set type = new.type,
        name = new.name,
        widget = new.widget,
        "lookupEntity" = new."lookupEntity",
        "lookupValueField" = new."lookupValueField",
        "lookupQuery" = new."lookupQuery",
        "masterFields" = new."masterFields",
        "childFields" = new."childFields",
        "childQuery" = new."childQuery",
        "default" = new."default",
        "conditionFieldId" = new."conditionFieldId",
        "conditionValues" = new."conditionValues",
        params = new.params,
        read_only = new.read_only,
        required = new.required,
        form_uuid = new.form_uuid,
        child_form_uuid = new.child_form_uuid,
        function = new.function,
        min = new.min,
        max = new.max
    where field_uuid = new.field_uuid;

    select count(*)>0 into new.entity_field from information_schema.columns where table_name=(select entity from form where form_uuid=new.form_uuid) and column_name=new.name;

    return new;
end;
$$;


create or replace function v_field_ins() returns trigger
    language plpgsql
    set search_path from current
as
$$
begin
    insert into field (type, name, widget, "lookupEntity", "lookupValueField", "lookupQuery", "masterFields", "childFields", "childQuery", "default", "conditionFieldId", "conditionValues", params, read_only, required, form_uuid, child_form_uuid, function,min,max) values
        (new.type, new.name, new.widget, new."lookupEntity", new."lookupValueField", new."lookupQuery", new."masterFields", new."childFields", new."childQuery", new."default", new."conditionFieldId", new."conditionValues", new.params, new.read_only, new.required, new.form_uuid, new.child_form_uuid, new.function,new.min,new.max)
    returning field_uuid into new.field_uuid;

    select count(*)>0 into new.entity_field from information_schema.columns where table_name=(select entity from form where form_uuid=new.form_uuid) and column_name=new.name;

    return new;
end;
$$;


create or replace function v_field_del() returns trigger
    language plpgsql
    set search_path from current
as
$$
begin
    delete from field where field_uuid = old.field_uuid;
    return old;
end;
$$;

create or replace function mail_notification(_mail_from email, _mail_to email[], _subject text, _text text, _html text, _params jsonb) returns uuid
    language sql
    set search_path from current
as
$$
select mail_notification(_mail_from,_mail_to,array[]::email[],array[]::email[],_subject,_text,_html,_params);
$$;


create or replace function mail_notification(_mail_from email, _mail_to email[], _subject text, _text text, _html text) returns uuid
    language sql
    set search_path from current
as
$$
select mail_notification(_mail_from,_mail_to,_subject,_text,_html,'{}'::jsonb);
$$;


create or replace function add_translator_user(_username text, _password text) returns void
    language plpgsql
    set search_path from current
as
$$
begin
    execute format('drop role if exists %I;',_username);
    execute format('create role %I with password %L;',_username,_password);
    execute format('grant box_translator to %I;',_username);
    execute format('alter role %I login;',_username);


end;
$$;


create or replace function mail_notification(_mail_from email, _mail_to email[], _mail_cc email[], _mail_bcc email[], _subject text, _text text, _html text, _params jsonb) returns uuid
    language plpgsql
    set search_path from current
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




create or replace function v_labels_update() returns trigger
    language plpgsql
    set search_path from current
as
$$
BEGIN
    UPDATE labels SET label = NEW.de WHERE key = NEW.key and lang='de';
    UPDATE labels SET label = NEW.it WHERE key = NEW.key and lang='it';
    RETURN NEW;
END $$;


create or replace function v_labels_insert() returns trigger
    language plpgsql
    set search_path from current
as
$$
BEGIN
    INSERT INTO labels (lang, key, label) values ('de',NEW.key,NEW.de),('it',NEW.key,NEW.it);
    RETURN NEW;
END $$;


