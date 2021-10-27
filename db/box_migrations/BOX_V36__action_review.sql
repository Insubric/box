alter table box.form_actions add column execute_function text;
alter table box.form_actions add column action_order double precision;

create table box.form_navigation_actions
(
    action text not null,
    importance text not null,
    after_action_goto text,
    label text not null,
    update_only boolean default false not null,
    insert_only boolean default false not null,
    reload boolean default false not null,
    confirm_text text,
    execute_function text,
    action_order double precision not null,
    uuid uuid default gen_random_uuid() not null
        constraint form_navigation_actions_pk
            primary key,
    form_uuid uuid not null
        constraint form_navigation_actions_form_form_id_fk
            references box.form
            on update cascade on delete cascade
);




INSERT INTO box.form_actions (action, importance, after_action_goto, label, update_only, insert_only, reload, confirm_text, form_uuid, execute_function,action_order)
select 'SaveAction', 'Primary', null, 'form.save', true, false, true, null, form_uuid, null,1
from box.form
where form_uuid not in (select distinct form_uuid from box.form_actions) and entity <> 'box_static_page';

INSERT INTO box.form_actions (action, importance, after_action_goto, label, update_only, insert_only, reload, confirm_text, form_uuid, execute_function,action_order)
select 'SaveAction', 'Primary', '/box/$kind/$name/row/$writable/$id', 'form.save', false, true, false, null, form_uuid, null,2
from box.form
where form_uuid not in (select distinct form_uuid from box.form_actions where action_order is null) and entity <> 'box_static_page';

INSERT INTO box.form_actions (action, importance, after_action_goto, label, update_only, insert_only, reload, confirm_text, form_uuid, execute_function,action_order)
select 'SaveAction', 'Std', '/box/$kind/$name', 'form.save_table', false, false, false, null, form_uuid, null,3
from box.form
where form_uuid not in (select distinct form_uuid from box.form_actions where action_order is null) and entity <> 'box_static_page';

INSERT INTO box.form_actions (action, importance, after_action_goto, label, update_only, insert_only, reload, confirm_text, form_uuid, execute_function,action_order)
select 'SaveAction', 'Std', '/box/$kind/$name/insert', 'form.save_add', false, false, false, null, form_uuid, null,4
from box.form
where form_uuid not in (select distinct form_uuid from box.form_actions where action_order is null) and entity <> 'box_static_page';

INSERT INTO box.form_actions (action, importance, after_action_goto, label, update_only, insert_only, reload, confirm_text, form_uuid, execute_function,action_order)
select 'NoAction', 'Primary', '/box/$kind/$name/insert', 'entity.new', false, false, false, null, form_uuid, null,5
from box.form
where form_uuid not in (select distinct form_uuid from box.form_actions where action_order is null) and entity <> 'box_static_page';

INSERT INTO box.form_actions (action, importance, after_action_goto, label, update_only, insert_only, reload, confirm_text, form_uuid, execute_function,action_order)
select 'CopyAction', 'Std', null, 'entity.duplicate', true, false, false, null, form_uuid, null,6
from box.form
where form_uuid not in (select distinct form_uuid from box.form_actions where action_order is null) and entity <> 'box_static_page';

INSERT INTO box.form_actions (action, importance, after_action_goto, label, update_only, insert_only, reload, confirm_text, form_uuid, execute_function,action_order)
select 'DeleteAction', 'Danger', '/box/$kind/$name', 'table.delete', true, false, false, 'table.confirmDelete', form_uuid, null,7
from box.form
where form_uuid not in (select distinct form_uuid from box.form_actions where action_order is null) and entity <> 'box_static_page';

INSERT INTO box.form_actions (action, importance, after_action_goto, label, update_only, insert_only, reload, confirm_text, form_uuid, execute_function,action_order)
select 'RevertAction', 'Std', null, 'table.revert', true, false, false, 'table.confirmRevert', form_uuid, null,8
from box.form
where form_uuid not in (select distinct form_uuid from box.form_actions where action_order is null) and entity <> 'box_static_page';

INSERT INTO box.form_navigation_actions (action, importance, after_action_goto, label, update_only, insert_only, reload, confirm_text, form_uuid, execute_function,action_order)
select 'NoAction', 'Std', '/box/$kind/$name', 'entity.table', false, false, false, null, form_uuid, null,1
from box.form
where entity <> 'box_static_page';


update box.form_actions set action_order=0 where action_order is null;

alter table box.form_actions alter column action_order set not null;

select * from box.form_navigation_actions

