create table if not exists "app_child" ("id" INTEGER NOT NULL PRIMARY KEY,"name" VARCHAR DEFAULT null,"parent_id" INTEGER DEFAULT null);
create table if not exists "app_parent" ("id" INTEGER NOT NULL PRIMARY KEY,"name" VARCHAR DEFAULT null);
create table if not exists "app_subchild" ("id" INTEGER NOT NULL PRIMARY KEY,"child_id" INTEGER DEFAULT null,"name" VARCHAR DEFAULT null);
create table if not exists "db_child" ("id" serial NOT NULL PRIMARY KEY,"name" VARCHAR DEFAULT null,"parent_id" INTEGER DEFAULT null);
create table if not exists "db_parent" ("id" serial NOT NULL PRIMARY KEY,"name" VARCHAR DEFAULT null);
create table if not exists "db_subchild" ("id" serial NOT NULL PRIMARY KEY,"child_id" INTEGER DEFAULT null,"name" VARCHAR DEFAULT null);
create table if not exists "simple" ("id" serial NOT NULL PRIMARY KEY,"name" VARCHAR DEFAULT null);

alter table "public"."db_child" add constraint "db_child_parent_id_fk" foreign key("parent_id") references "db_parent"("id") on update NO ACTION on delete NO ACTION;
alter table "public"."db_subchild" add constraint "db_subchild_child_id_fk" foreign key("child_id") references "db_child"("id") on update NO ACTION on delete NO ACTION;
alter table "public"."app_child" add constraint "app_child_parent_id_fk" foreign key("parent_id") references "app_parent"("id") on update NO ACTION on delete NO ACTION;
alter table "public"."app_subchild" add constraint "app_subchild_child_id_fk" foreign key("child_id") references "app_child"("id") on update NO ACTION on delete NO ACTION;


create table test_list_types(
    id serial,
    texts text[],
    ints int[],
    numbers double precision[]
);