
alter table field add column foreign_entity text;
alter table field add column foreign_value_field text;
alter table field add column foreign_key_columns text[];
alter table field add column local_key_columns text[];

alter table field_i18n add column foreign_label_columns text[];
alter table field_i18n add column dynamic_label text;

update field set foreign_entity="lookupEntity", foreign_value_field="lookupValueField" where field_uuid is not null;

update field f set foreign_key_columns=ext.columns
from (
    select field_uuid,array_agg(trim(e)) columns
        from (
            select field_uuid,
                unnest(string_to_array("childFields",',')) e
            from field
        ) t
    group by field_uuid
) ext
where f.field_uuid = ext.field_uuid;

update field f set local_key_columns=ext.columns
from (
         select field_uuid,array_agg(trim(e)) columns
         from (
                  select field_uuid,
                         unnest(string_to_array("masterFields",',')) e
                  from field
              ) t
         group by field_uuid
     ) ext
where f.field_uuid = ext.field_uuid;

update field_i18n f set foreign_label_columns=ext.columns
from (
         select uuid,array_agg(trim(e)) columns
         from (
                  select uuid,
                         unnest(string_to_array("lookupTextField",',')) e
                  from field_i18n
              ) t
         group by uuid
     ) ext
where f.uuid = ext.uuid and f.field_uuid in (select field_uuid from field where field.foreign_entity is not null);

update field_i18n f set dynamic_label="lookupTextField" where f."lookupTextField" is not null and f.field_uuid not in (select field_uuid from field where field.foreign_entity is not null);

update field f set foreign_value_field=t."lookupTextField"
from (select distinct field_uuid,"lookupTextField" from field_i18n where "lookupTextField" is not null) t
where t.field_uuid = f.field_uuid and widget in ('linked_form','lookup_form');

drop view v_box_usages;
drop view v_field;
alter table field drop "lookupEntity";
alter table field drop "lookupValueField";
alter table field drop "masterFields";
alter table field drop "childFields";
