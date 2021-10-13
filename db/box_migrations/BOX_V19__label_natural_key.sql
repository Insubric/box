
alter table box.labels drop constraint if exists labels_pkey;

alter table box.labels drop column if exists id;

alter table box.labels
    add constraint labels_pkey
        primary key (lang, key);