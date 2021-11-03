create table box.form_actions
(
    id serial
        constraint form_actions_pk
            primary key,
    form_id int not null
        constraint form_actions_form_form_id_fk
            references box.form,
    action text not null,
    importance text not null,
    after_action_goto text,
    label text not null,
    update_only boolean default false not null,
    insert_only boolean default false not null,
    reload boolean default false not null,
    confirm_text text
);

