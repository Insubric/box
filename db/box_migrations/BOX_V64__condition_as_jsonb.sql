
drop view v_field;
alter table field add column condition jsonb;
update field
set condition = (
    case
        when jsonb_typeof("conditionValues"::jsonb) = 'object' then jsonb_build_object('field', "conditionFieldId",
                                                                                       'condition',
                                                                                       jsonb_build_object('not',
                                                                                                          jsonb_build_object(
                                                                                                                  'or',
                                                                                                                  (select jsonb_agg(jsonb_build_object('value', value))
                                                                                                                   from jsonb_array_elements("conditionValues"::jsonb -> 'not')))))
        when jsonb_typeof("conditionValues"::jsonb) = 'array' then jsonb_build_object('field', "conditionFieldId",
                                                                                      'condition',
                                                                                      jsonb_build_object('or',
                                                                                                         (select jsonb_agg(jsonb_build_object('value', value))
                                                                                                          from jsonb_array_elements("conditionValues"::jsonb))))
        else jsonb_build_object('field', "conditionFieldId", 'condition',
                                jsonb_build_object('value', "conditionValues"::jsonb))
        end
    )
where "conditionValues" is not null and "conditionFieldId" is not null;

alter table field drop "conditionFieldId";
alter table field drop "conditionValues";

alter table function_field add column condition jsonb;
update function_field
set condition = (
    case
        when jsonb_typeof("conditionValues"::jsonb) = 'object' then jsonb_build_object('field', "conditionFieldId",
                                                                                       'condition',
                                                                                       jsonb_build_object('not',
                                                                                                          jsonb_build_object(
                                                                                                                  'or',
                                                                                                                  (select jsonb_agg(jsonb_build_object('value', value))
                                                                                                                   from jsonb_array_elements("conditionValues"::jsonb -> 'not')))))
        when jsonb_typeof("conditionValues"::jsonb) = 'array' then jsonb_build_object('field', "conditionFieldId",
                                                                                      'condition',
                                                                                      jsonb_build_object('or',
                                                                                                         (select jsonb_agg(jsonb_build_object('value', value))
                                                                                                          from jsonb_array_elements("conditionValues"::jsonb))))
        else jsonb_build_object('field', "conditionFieldId", 'condition',
                                jsonb_build_object('value', "conditionValues"::jsonb))
        end
    )
where "conditionValues" is not null and "conditionFieldId" is not null;

alter table function_field drop "conditionFieldId";
alter table function_field drop "conditionValues";



alter table export_field add column condition jsonb;
update export_field
set condition = (
    case
        when jsonb_typeof("conditionValues"::jsonb) = 'object' then jsonb_build_object('field', "conditionFieldId",
                                                                                       'condition',
                                                                                       jsonb_build_object('not',
                                                                                                          jsonb_build_object(
                                                                                                                  'or',
                                                                                                                  (select jsonb_agg(jsonb_build_object('value', value))
                                                                                                                   from jsonb_array_elements("conditionValues"::jsonb -> 'not')))))
        when jsonb_typeof("conditionValues"::jsonb) = 'array' then jsonb_build_object('field', "conditionFieldId",
                                                                                      'condition',
                                                                                      jsonb_build_object('or',
                                                                                                         (select jsonb_agg(jsonb_build_object('value', value))
                                                                                                          from jsonb_array_elements("conditionValues"::jsonb))))
        else jsonb_build_object('field', "conditionFieldId", 'condition',
                                jsonb_build_object('value', "conditionValues"::jsonb))
        end
    )
where "conditionValues" is not null and "conditionFieldId" is not null;

alter table export_field drop "conditionFieldId";
alter table export_field drop "conditionValues";




update form_actions fa set condition=c2.condition
from (select uuid, jsonb_build_object('field', key, 'condition', jsonb_build_object('value', value)) as condition
      from form_actions, jsonb_each(condition)
      where jsonb_typeof(condition) = 'object') c2
where c2.uuid = fa.uuid;

update form_actions_table fa set condition=c2.condition
from (select uuid, jsonb_build_object('field', key, 'condition', jsonb_build_object('value', value)) as condition
      from form_actions_table, jsonb_each(condition)
      where jsonb_typeof(condition) = 'object') c2
where c2.uuid = fa.uuid;

update form_actions_top_table fa set condition=c2.condition
from (select uuid, jsonb_build_object('field', key, 'condition', jsonb_build_object('value', value)) as condition
      from form_actions_top_table, jsonb_each(condition)
      where jsonb_typeof(condition) = 'object') c2
where c2.uuid = fa.uuid;