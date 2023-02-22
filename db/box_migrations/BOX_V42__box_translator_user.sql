do
$do$
    begin
        if exists (select from pg_roles where rolname = 'box_translator') then
            raise notice 'skip box_translation creation';
        else
            create role box_translator;
        end if;
    end
$do$;

grant select,update,insert on form_i18n to box_translator;
grant select,update,insert on field_i18n to box_translator;


create function add_translator_user(_username text, _password text) returns void
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