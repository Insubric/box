
-- Cleaning
delete from field_i18n where id in
                                 (select id from field_i18n fi
                                                     left join field f on f.field_id=fi.field_id
                                  where f.field_id is null);

-- FORMS
alter table form add column form_uuid uuid not null default gen_random_uuid();

alter table form_i18n add column uuid uuid not null default gen_random_uuid();
alter table form_i18n add column form_uuid uuid;

alter table form_actions add column uuid uuid not null default gen_random_uuid();
alter table form_actions add column form_uuid uuid;

alter table field add column field_uuid uuid not null default gen_random_uuid();
alter table field add column form_uuid uuid;
alter table field add column child_form_uuid uuid;

alter table field_i18n add column uuid uuid not null default gen_random_uuid();
alter table field_i18n add column field_uuid uuid;

alter table field_file add column field_uuid uuid;

update form_i18n as i
set form_uuid = f.form_uuid
from form as f
where i.form_id = f.form_id;

alter table form_i18n alter column form_uuid set not null;

update form_actions as i
set form_uuid = f.form_uuid
from form as f
where i.form_id = f.form_id;

alter table form_actions alter column form_uuid set not null;

update field as i
set form_uuid = f.form_uuid
from form as f
where i.form_id = f.form_id;

alter table field alter column form_uuid set not null;

update field as i
set child_form_uuid = f.form_uuid
from form as f
where i.child_form_id = f.form_id;

delete from field_i18n where field_id is null;

update field_i18n as i
set field_uuid = f.field_uuid
from field as f
where i.field_id = f.field_id;

alter table field_i18n alter column field_uuid set not null;

update field_file as i
set field_uuid = f.field_uuid
from field as f
where i.field_id = f.field_id;

alter table field_file alter column field_uuid set not null;

alter table field_i18n drop constraint if exists fkey_field cascade;
alter table field_file drop constraint if exists field_file_fielf_id_fk cascade;
alter table field drop constraint if exists fkey_form cascade;
alter table form_actions drop constraint form_actions_form_form_id_fk cascade;
alter table form_i18n drop constraint if exists fkey_form;

alter table form drop constraint if exists form_pkey cascade;
alter table form drop constraint if exists form_pk cascade;
alter table form add constraint form_pkey primary key (form_uuid);

alter table field drop constraint if exists field_pkey cascade;
alter table field drop constraint if exists field_pk cascade;
alter table field add constraint field_pkey primary key (field_uuid);
alter table field add constraint fkey_form foreign key (form_uuid) references form (form_uuid) on update cascade on delete cascade;
alter table field add constraint fkey_form_child foreign key (child_form_uuid) references form (form_uuid) on update no action on delete no action;

alter table field_i18n drop constraint if exists field_i18n_pkey cascade;
alter table field_i18n drop constraint if exists field_i18n_pk cascade;
alter table field_i18n add constraint field_i18n_pkey primary key (uuid);
alter table field_i18n add constraint fkey_field foreign key (field_uuid) references field (field_uuid) on update cascade on delete cascade;

alter table field_file drop constraint if exists field_file_pkey cascade;
alter table field_file drop constraint if exists field_file_pk cascade;
alter table field_file add constraint field_file_pkey primary key (field_uuid);
alter table field_file add constraint field_file_fielf_id_fk foreign key (field_uuid) references field (field_uuid) on update cascade on delete cascade;

alter table form_actions drop constraint form_actions_pk cascade;
alter table form_actions add constraint form_actions_pk primary key (uuid);
alter table form_actions add constraint form_actions_form_form_id_fk foreign key (form_uuid) references form (form_uuid) on update cascade on delete cascade;

alter table form_i18n drop constraint if exists form_i18n_pkey cascade;
alter table form_i18n drop constraint if exists form_i18n_pk cascade;
alter table form_i18n add constraint form_i18n_pkey primary key (uuid);
alter table form_i18n add constraint fkey_form foreign key (form_uuid) references form (form_uuid) on update cascade on delete cascade;



alter table form drop column form_id;

alter table form_i18n drop column id;
alter table form_i18n drop column form_id;

alter table form_actions drop column id;
alter table form_actions drop column form_id;

alter table field drop column field_id;
alter table field drop column form_id;
alter table field drop column child_form_id;

alter table field_i18n drop column id;
alter table field_i18n drop column field_id;

alter table field_file drop column field_id;





-- FUNCTIONS

alter table function add column function_uuid uuid not null default gen_random_uuid();

alter table function_i18n add column uuid uuid not null default gen_random_uuid();
alter table function_i18n add column function_uuid uuid;

alter table function_field add column field_uuid uuid not null default gen_random_uuid();
alter table function_field add column function_uuid uuid;

alter table function_field_i18n add column uuid uuid not null default gen_random_uuid();
alter table function_field_i18n add column field_uuid uuid;


update function_i18n as i
set function_uuid = f.function_uuid
from function as f
where i.function_id = f.function_id;

alter table function_i18n alter column function_uuid set not null;

update function_field as i
set function_uuid = f.function_uuid
from function as f
where i.function_id = f.function_id;

alter table function_field alter column function_uuid set not null;

update function_field_i18n as i
set field_uuid = f.field_uuid
from function_field as f
where i.field_id = f.field_id;

alter table function_field_i18n alter column field_uuid set not null;


alter table function_i18n drop constraint if exists fkey_function cascade;
alter table function_field drop constraint if exists fkey_function cascade;
alter table function_field_i18n drop constraint if exists fkey_field cascade;


alter table function drop constraint if exists function_pkey cascade;
alter table function drop constraint if exists function_pk cascade;
alter table function add constraint function_pkey primary key (function_uuid);

alter table function_i18n drop constraint if exists function_i18n_pkey cascade;
alter table function_i18n drop constraint if exists function_i18n_pk cascade;
alter table function_i18n add constraint function_i18n_pkey primary key (uuid);
alter table function_i18n add constraint fkey_form foreign key (function_uuid) references function (function_uuid) on update cascade on delete cascade;

alter table function_field drop constraint if exists function_field_pkey cascade;
alter table function_field drop constraint if exists function_field_pk cascade;
alter table function_field add constraint function_field_pkey primary key (field_uuid);
alter table function_field add constraint fkey_form foreign key (function_uuid) references function (function_uuid) on update cascade on delete cascade;

alter table function_field_i18n drop constraint if exists function_field_i18n_pkey cascade;
alter table function_field_i18n drop constraint if exists function_field_118n_pk cascade;
alter table function_field_i18n drop constraint if exists function_field_i18n_pk cascade;
alter table function_field_i18n add constraint function_field_i18n_pkey primary key (uuid);
alter table function_field_i18n add constraint fkey_field foreign key (field_uuid) references function_field (field_uuid) on update cascade on delete cascade;


alter table function drop column function_id;

alter table function_i18n drop column id;
alter table function_i18n drop column function_id;

alter table function_field drop column field_id;
alter table function_field drop column function_id;

alter table function_field_i18n drop column id;
alter table function_field_i18n drop column field_id;

-- EXPORTS

alter table export add column export_uuid uuid not null default gen_random_uuid();

alter table export_i18n add column uuid uuid not null default gen_random_uuid();
alter table export_i18n add column export_uuid uuid;

alter table export_field add column field_uuid uuid not null default gen_random_uuid();
alter table export_field add column export_uuid uuid;

alter table export_field_i18n add column uuid uuid not null default gen_random_uuid();
alter table export_field_i18n add column field_uuid uuid;

update export_i18n as i
set export_uuid = f.export_uuid
from export as f
where i.export_id = f.export_id;

alter table export_i18n alter column export_uuid set not null;

update export_field as i
set export_uuid = f.export_uuid
from export as f
where i.export_id = f.export_id;

alter table export_field alter column export_uuid set not null;

update export_field_i18n as i
set field_uuid = f.field_uuid
from export_field as f
where i.field_id = f.field_id;

alter table export_field_i18n alter column field_uuid set not null;

alter table export_i18n drop constraint if exists fkey_export cascade;
alter table export_field drop constraint if exists fkey_export cascade;
alter table export_field_i18n drop constraint if exists fkey_field cascade;

alter table export drop constraint if exists export_pkey cascade;
alter table export add constraint export_pkey primary key (export_uuid);

alter table export_i18n drop constraint if exists export_i18n_pk cascade;
alter table export_i18n drop constraint if exists export_i18n_pkey cascade;
alter table export_i18n add constraint export_i18n_pkey primary key (uuid);
alter table export_i18n add constraint fkey_form foreign key (export_uuid) references export (export_uuid) on update cascade on delete cascade;

alter table export_field drop constraint if exists export_field_pk cascade;
alter table export_field drop constraint if exists export_field_pkey cascade;
alter table export_field add constraint export_field_pkey primary key (field_uuid);
alter table export_field add constraint fkey_form foreign key (export_uuid) references export (export_uuid) on update cascade on delete cascade;

alter table export_field_i18n drop constraint if exists export_field_i18n_pkey cascade;
alter table export_field_i18n drop constraint if exists export_field_i18n_pk cascade;
alter table export_field_i18n add constraint export_field_i18n_pkey primary key (uuid);
alter table export_field_i18n add constraint fkey_field foreign key (field_uuid) references export_field (field_uuid) on update cascade on delete cascade;


alter table export drop column export_id;

alter table export_i18n drop column id;
alter table export_i18n drop column export_id;

alter table export_field drop column field_id;
alter table export_field drop column export_id;

alter table export_field_i18n drop column id;
alter table export_field_i18n drop column field_id;

-- OTHERS

alter table news add column news_uuid uuid not null default gen_random_uuid();

alter table news_i18n add column news_uuid uuid;

update news_i18n as i
set news_uuid = f.news_uuid
from news as f
where i.news_id = f.news_id;

alter table news_i18n alter column news_uuid set not null;

alter table news_i18n drop constraint fkey_news_i18n cascade;

alter table news drop constraint news_pkey cascade;
alter table news add constraint news_pkey primary key (news_uuid);

alter table news_i18n drop constraint news_i18n_pkey cascade;
alter table news_i18n add constraint news_i18n_pkey primary key (news_uuid,lang);
alter table news_i18n add constraint fkey_news_i18n foreign key (news_uuid) references news (news_uuid) on update cascade on delete cascade;

alter table news drop column news_id;
alter table news_i18n drop column news_id;



alter table ui_src add column uuid uuid not null default gen_random_uuid();

alter table ui_src drop constraint if exists ui_src_pkey cascade;
alter table ui_src drop constraint if exists ui_src_pk cascade;
alter table ui_src add constraint ui_src_pkey primary key (uuid);
alter table ui_src drop column id;