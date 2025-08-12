
drop view if exists v_field;
alter table field alter "lookupQuery" type jsonb using "lookupQuery"::jsonb;
alter table field alter "childQuery" type jsonb using "childQuery"::jsonb;
alter table export_field alter "lookupQuery" type jsonb using "lookupQuery"::jsonb;