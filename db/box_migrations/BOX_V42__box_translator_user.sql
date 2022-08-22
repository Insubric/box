create role box_translator;
grant select,update,insert on box.form_i18n to box_translator;
grant select,update,insert on box.field_i18n to box_translator;
grant select,update,insert on box.v_labels to box_translator;

create function box.add_translator_user(_username text, _password text) returns void
    language plpgsql
as
$$
begin
    execute format('drop role if exists %I;',_username);
    execute format('create role %I with password %L;',_username,_password);
    execute format('grant box_translator to %I;',_username);
    execute format('alter role %I login;',_username);


end;
$$;