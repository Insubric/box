alter table box.form
    drop column view_id;

alter table box.form
	drop column view_table;

alter table box.form_i18n
	add view_table text;