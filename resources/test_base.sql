create table if not exists "app_child" ("id" INTEGER NOT NULL PRIMARY KEY,"name" VARCHAR DEFAULT null,"parent_id" INTEGER DEFAULT null);
create table if not exists "app_parent" ("id" INTEGER NOT NULL PRIMARY KEY,"name" VARCHAR DEFAULT null);
create table if not exists "app_subchild" ("id" INTEGER NOT NULL PRIMARY KEY,"child_id" INTEGER DEFAULT null,"name" VARCHAR DEFAULT null);
create table if not exists "db_child" ("id" serial NOT NULL PRIMARY KEY,"name" VARCHAR DEFAULT null,"parent_id" INTEGER DEFAULT null);
create table if not exists "db_parent" ("id" serial NOT NULL PRIMARY KEY,"name" VARCHAR DEFAULT null);
create table if not exists "db_subchild" ("id" serial NOT NULL PRIMARY KEY,"child_id" INTEGER DEFAULT null,"name" VARCHAR DEFAULT null);
create table if not exists "simple" ("id" serial NOT NULL PRIMARY KEY,"name" VARCHAR DEFAULT null, name2 text);

alter table "db_child" add constraint "db_child_parent_id_fk" foreign key("parent_id") references "db_parent"("id") on update NO ACTION on delete NO ACTION;
alter table "db_subchild" add constraint "db_subchild_child_id_fk" foreign key("child_id") references "db_child"("id") on update NO ACTION on delete NO ACTION;
alter table "app_child" add constraint "app_child_parent_id_fk" foreign key("parent_id") references "app_parent"("id") on update NO ACTION on delete NO ACTION;
alter table "app_subchild" add constraint "app_subchild_child_id_fk" foreign key("child_id") references "app_child"("id") on update NO ACTION on delete NO ACTION;


create table test_list_types(
    id serial,
    texts text[],
    ints int[],
    numbers double precision[]
);

create table json_test(
        id serial,
        obj jsonb
);

create table ce
(
    id                   serial primary key
);

create table ces
(
    ce_id integer                        not null
        constraint ces_fk
            references ce
            on update cascade on delete cascade,
    s_id    integer                        not null,
    negative boolean default true,
    constraint ces_pk
        primary key (ce_id, s_id)
);

create table cesr
(
    ce_id integer                        not null,
    s_id    integer                        not null,
    p_id       text                           not null,
    primary key (ce_id, s_id, p_id),
    foreign key (ce_id, s_id) references ces
        on update cascade on delete cascade
);