
alter table box.conf drop constraint if exists conf_pkey;

alter table box.conf drop column if exists id;

alter table box.conf
    add constraint conf_pkey
        primary key (key);


alter table box.ui drop constraint if exists ui_pkey;

alter table box.ui drop column if exists id;

alter table box.ui
    add constraint ui_pkey
        primary key (access_level_id, key);