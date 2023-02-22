
alter table labels drop constraint if exists labels_pkey;

alter table labels drop column if exists id;

alter table labels
    add constraint labels_pkey
        primary key (lang, key);