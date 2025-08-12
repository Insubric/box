insert into labels (lang, key, label)
select lang,'export-header.'|| key,label from export_header_i18n;

drop table export_header_i18n;