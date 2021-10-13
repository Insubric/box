
-- Cleaning
delete from box.field_i18n where id in
                                 (select id from box.field_i18n fi
                                                     left join box.field f on f.field_id=fi.field_id
                                  where f.field_id is null);

-- FORMS
alter table box.form add column form_uuid uuid not null default gen_random_uuid();

alter table box.form_i18n add column uuid uuid not null default gen_random_uuid();
alter table box.form_i18n add column form_uuid uuid;

alter table box.form_actions add column uuid uuid not null default gen_random_uuid();
alter table box.form_actions add column form_uuid uuid;

alter table box.field add column field_uuid uuid not null default gen_random_uuid();
alter table box.field add column form_uuid uuid;
alter table box.field add column child_form_uuid uuid;

alter table box.field_i18n add column uuid uuid not null default gen_random_uuid();
alter table box.field_i18n add column field_uuid uuid;

alter table box.field_file add column field_uuid uuid;

update box.form_i18n as i
set form_uuid = f.form_uuid
from box.form as f
where i.form_id = f.form_id;

alter table box.form_i18n alter column form_uuid set not null;

update box.form_actions as i
set form_uuid = f.form_uuid
from box.form as f
where i.form_id = f.form_id;

alter table box.form_actions alter column form_uuid set not null;

update box.field as i
set form_uuid = f.form_uuid
from box.form as f
where i.form_id = f.form_id;

alter table box.field alter column form_uuid set not null;

update box.field as i
set child_form_uuid = f.form_uuid
from box.form as f
where i.child_form_id = f.form_id;

delete from box.field_i18n where field_id is null;

update box.field_i18n as i
set field_uuid = f.field_uuid
from box.field as f
where i.field_id = f.field_id;

alter table box.field_i18n alter column field_uuid set not null;

update box.field_file as i
set field_uuid = f.field_uuid
from box.field as f
where i.field_id = f.field_id;

alter table box.field_file alter column field_uuid set not null;

alter table box.field_i18n drop constraint if exists fkey_field cascade;
alter table box.field_file drop constraint if exists field_file_fielf_id_fk cascade;
alter table box.field drop constraint if exists fkey_form cascade;
alter table box.form_actions drop constraint form_actions_form_form_id_fk cascade;
alter table box.form_i18n drop constraint if exists fkey_form;

alter table box.form drop constraint if exists form_pkey cascade;
alter table box.form drop constraint if exists form_pk cascade;
alter table box.form add constraint form_pkey primary key (form_uuid);

alter table box.field drop constraint if exists field_pkey cascade;
alter table box.field drop constraint if exists field_pk cascade;
alter table box.field add constraint field_pkey primary key (field_uuid);
alter table box.field add constraint fkey_form foreign key (form_uuid) references box.form (form_uuid) on update cascade on delete cascade;
alter table box.field add constraint fkey_form_child foreign key (child_form_uuid) references box.form (form_uuid) on update no action on delete no action;

alter table box.field_i18n drop constraint if exists field_i18n_pkey cascade;
alter table box.field_i18n drop constraint if exists field_i18n_pk cascade;
alter table box.field_i18n add constraint field_i18n_pkey primary key (uuid);
alter table box.field_i18n add constraint fkey_field foreign key (field_uuid) references box.field (field_uuid) on update cascade on delete cascade;

alter table box.field_file drop constraint if exists field_file_pkey cascade;
alter table box.field_file drop constraint if exists field_file_pk cascade;
alter table box.field_file add constraint field_file_pkey primary key (field_uuid);
alter table box.field_file add constraint field_file_fielf_id_fk foreign key (field_uuid) references box.field (field_uuid) on update cascade on delete cascade;

alter table box.form_actions drop constraint form_actions_pk cascade;
alter table box.form_actions add constraint form_actions_pk primary key (uuid);
alter table box.form_actions add constraint form_actions_form_form_id_fk foreign key (form_uuid) references box.form (form_uuid) on update cascade on delete cascade;

alter table box.form_i18n drop constraint if exists form_i18n_pkey cascade;
alter table box.form_i18n drop constraint if exists form_i18n_pk cascade;
alter table box.form_i18n add constraint form_i18n_pkey primary key (uuid);
alter table box.form_i18n add constraint fkey_form foreign key (form_uuid) references box.form (form_uuid) on update cascade on delete cascade;



alter table box.form drop column form_id;

alter table box.form_i18n drop column id;
alter table box.form_i18n drop column form_id;

alter table box.form_actions drop column id;
alter table box.form_actions drop column form_id;

alter table box.field drop column field_id;
alter table box.field drop column form_id;
alter table box.field drop column child_form_id;

alter table box.field_i18n drop column id;
alter table box.field_i18n drop column field_id;

alter table box.field_file drop column field_id;





-- FUNCTIONS

alter table box.function add column function_uuid uuid not null default gen_random_uuid();

alter table box.function_i18n add column uuid uuid not null default gen_random_uuid();
alter table box.function_i18n add column function_uuid uuid;

alter table box.function_field add column field_uuid uuid not null default gen_random_uuid();
alter table box.function_field add column function_uuid uuid;

alter table box.function_field_i18n add column uuid uuid not null default gen_random_uuid();
alter table box.function_field_i18n add column field_uuid uuid;


update box.function_i18n as i
set function_uuid = f.function_uuid
from box.function as f
where i.function_id = f.function_id;

alter table box.function_i18n alter column function_uuid set not null;

update box.function_field as i
set function_uuid = f.function_uuid
from box.function as f
where i.function_id = f.function_id;

alter table box.function_field alter column function_uuid set not null;

update box.function_field_i18n as i
set field_uuid = f.field_uuid
from box.function_field as f
where i.field_id = f.field_id;

alter table box.function_field_i18n alter column field_uuid set not null;


alter table box.function_i18n drop constraint if exists fkey_function cascade;
alter table box.function_field drop constraint if exists fkey_function cascade;
alter table box.function_field_i18n drop constraint if exists fkey_field cascade;


alter table box.function drop constraint if exists function_pkey cascade;
alter table box.function drop constraint if exists function_pk cascade;
alter table box.function add constraint function_pkey primary key (function_uuid);

alter table box.function_i18n drop constraint if exists function_i18n_pkey cascade;
alter table box.function_i18n drop constraint if exists function_i18n_pk cascade;
alter table box.function_i18n add constraint function_i18n_pkey primary key (uuid);
alter table box.function_i18n add constraint fkey_form foreign key (function_uuid) references box.function (function_uuid) on update cascade on delete cascade;

alter table box.function_field drop constraint if exists function_field_pkey cascade;
alter table box.function_field drop constraint if exists function_field_pk cascade;
alter table box.function_field add constraint function_field_pkey primary key (field_uuid);
alter table box.function_field add constraint fkey_form foreign key (function_uuid) references box.function (function_uuid) on update cascade on delete cascade;

alter table box.function_field_i18n drop constraint if exists function_field_i18n_pkey cascade;
alter table box.function_field_i18n drop constraint if exists function_field_118n_pk cascade;
alter table box.function_field_i18n drop constraint if exists function_field_i18n_pk cascade;
alter table box.function_field_i18n add constraint function_field_i18n_pkey primary key (uuid);
alter table box.function_field_i18n add constraint fkey_field foreign key (field_uuid) references box.function_field (field_uuid) on update cascade on delete cascade;


alter table box.function drop column function_id;

alter table box.function_i18n drop column id;
alter table box.function_i18n drop column function_id;

alter table box.function_field drop column field_id;
alter table box.function_field drop column function_id;

alter table box.function_field_i18n drop column id;
alter table box.function_field_i18n drop column field_id;

-- EXPORTS

alter table box.export add column export_uuid uuid not null default gen_random_uuid();

alter table box.export_i18n add column uuid uuid not null default gen_random_uuid();
alter table box.export_i18n add column export_uuid uuid;

alter table box.export_header_i18n add column uuid uuid not null default gen_random_uuid();
alter table box.export_header_i18n add column export_uuid uuid;

alter table box.export_field add column field_uuid uuid not null default gen_random_uuid();
alter table box.export_field add column export_uuid uuid;

alter table box.export_field_i18n add column uuid uuid not null default gen_random_uuid();
alter table box.export_field_i18n add column field_uuid uuid;

update box.export_i18n as i
set export_uuid = f.export_uuid
from box.export as f
where i.export_id = f.export_id;

alter table box.export_i18n alter column export_uuid set not null;

update box.export_field as i
set export_uuid = f.export_uuid
from box.export as f
where i.export_id = f.export_id;

alter table box.export_field alter column export_uuid set not null;

update box.export_header_i18n as i
set export_uuid = f.export_uuid
from box.export as f
where i.id = f.export_id;

alter table box.export_header_i18n alter column export_uuid set not null;

update box.export_field_i18n as i
set field_uuid = f.field_uuid
from box.export_field as f
where i.field_id = f.field_id;

alter table box.export_field_i18n alter column field_uuid set not null;

alter table box.export_i18n drop constraint if exists fkey_export cascade;
alter table box.export_field drop constraint if exists fkey_export cascade;
alter table box.export_field_i18n drop constraint if exists fkey_field cascade;


alter table box.export drop constraint if exists export_pkey cascade;
alter table box.export add constraint export_pkey primary key (export_uuid);

alter table box.export_i18n drop constraint if exists export_i18n_pk cascade;
alter table box.export_i18n drop constraint if exists export_i18n_pkey cascade;
alter table box.export_i18n add constraint export_i18n_pkey primary key (uuid);
alter table box.export_i18n add constraint fkey_form foreign key (export_uuid) references box.export (export_uuid) on update cascade on delete cascade;

alter table box.export_field drop constraint if exists export_field_pk cascade;
alter table box.export_field drop constraint if exists export_field_pkey cascade;
alter table box.export_field add constraint export_field_pkey primary key (field_uuid);
alter table box.export_field add constraint fkey_form foreign key (export_uuid) references box.export (export_uuid) on update cascade on delete cascade;

alter table box.export_field_i18n drop constraint if exists export_field_i18n_pkey cascade;
alter table box.export_field_i18n drop constraint if exists export_field_i18n_pk cascade;
alter table box.export_field_i18n add constraint export_field_i18n_pkey primary key (uuid);
alter table box.export_field_i18n add constraint fkey_field foreign key (field_uuid) references box.export_field (field_uuid) on update cascade on delete cascade;


alter table box.export drop column export_id;

alter table box.export_i18n drop column id;
alter table box.export_i18n drop column export_id;

alter table box.export_field drop column field_id;
alter table box.export_field drop column export_id;

alter table box.export_field_i18n drop column id;
alter table box.export_field_i18n drop column field_id;

-- OTHERS

alter table box.news add column news_uuid uuid not null default gen_random_uuid();

alter table box.news_i18n add column news_uuid uuid;

update box.news_i18n as i
set news_uuid = f.news_uuid
from box.news as f
where i.news_id = f.news_id;

alter table box.news_i18n alter column news_uuid set not null;

alter table box.news_i18n drop constraint fkey_news_i18n cascade;

alter table box.news drop constraint news_pkey cascade;
alter table box.news add constraint news_pkey primary key (news_uuid);

alter table box.news_i18n drop constraint news_i18n_pkey cascade;
alter table box.news_i18n add constraint news_i18n_pkey primary key (news_uuid,lang);
alter table box.news_i18n add constraint fkey_news_i18n foreign key (news_uuid) references box.news (news_uuid) on update cascade on delete cascade;

alter table box.news drop column news_id;
alter table box.news_i18n drop column news_id;



alter table box.ui_src add column uuid uuid not null default gen_random_uuid();

alter table box.ui_src drop constraint if exists ui_src_pkey cascade;
alter table box.ui_src drop constraint if exists ui_src_pk cascade;
alter table box.ui_src add constraint ui_src_pkey primary key (uuid);
alter table box.ui_src drop column id;
