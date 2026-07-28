
alter table form_actions add column enabled_roles text[];
alter table form_actions_table add column enabled_roles text[];
alter table form_actions_top_table add column enabled_roles text[];
alter table form_navigation_actions add column enabled_roles text[];

alter table form_actions add column view_enable boolean not null default false;
alter table form_actions add column edit_enable boolean not null default true;
alter table form_navigation_actions add column view_enable boolean not null default false;
alter table form_navigation_actions add column edit_enable boolean not null default true;

