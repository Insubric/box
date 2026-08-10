
alter table form_actions add need_update_right boolean default false not null;
alter table form_actions add need_delete_right boolean default false not null;

alter table form_navigation_actions add need_update_right boolean default false not null;
alter table form_navigation_actions add need_delete_right boolean default false not null;