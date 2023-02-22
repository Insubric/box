
alter table form_actions_table add column target text;

create table form_actions_top_table
(
    action            text                              not null,
    importance        text                              not null,
    after_action_goto text,
    label             text                              not null,
    confirm_text      text,
    uuid              uuid    default gen_random_uuid() not null
        constraint form_actions_top_table_pk
            primary key,
    form_uuid         uuid                              not null
        constraint form_actions_top_table_form_form_id_fk
            references form
            on update cascade on delete cascade,
    execute_function  text,
    action_order      double precision                  not null,
    condition         jsonb,
    need_update_right boolean default false  not null,
    need_delete_right boolean default false  not null,
    need_insert_right boolean default false  not null,
    when_no_update_right boolean default false  not null,
    target text
);

alter table form_actions_top_table
    owner to postgres;