alter table box.form
	add column if not exists view_table text;

alter table box.form
	add column if not exists view_id text;
