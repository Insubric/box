alter table form alter "tabularFields" type text[] using string_to_array("tabularFields",',');
alter table form alter "exportfields" type text[] using string_to_array("exportfields",',');

