
alter table conf drop constraint if exists conf_pkey;

alter table conf drop column if exists id;

alter table conf
    add constraint conf_pkey
        primary key (key);


alter table ui drop constraint if exists ui_pkey;

alter table ui drop column if exists id;

alter table ui
    add constraint ui_pkey
        primary key (access_level_id, key);