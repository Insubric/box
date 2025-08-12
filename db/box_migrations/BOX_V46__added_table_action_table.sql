
create table form_actions_table
(
    action            text                              not null,
    importance        text                              not null,
    after_action_goto text,
    label             text                              not null,
    update_only       boolean default false             not null,
    insert_only       boolean default false             not null,
    reload            boolean default false             not null,
    confirm_text      text,
    uuid              uuid    default gen_random_uuid() not null
        constraint form_actions_table_pk
            primary key,
    form_uuid         uuid                              not null
        constraint form_actions_table_form_form_id_fk
            references form
            on update cascade on delete cascade,
    execute_function  text,
    action_order      double precision                  not null,
    condition         jsonb,
    html_check        boolean default true              not null,
    need_update_right boolean default false  not null,
    need_delete_right boolean default false  not null,
    when_no_update_right boolean default false  not null
);


