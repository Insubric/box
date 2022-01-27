insert into box.labels (lang, key, label)
select lang,'export-header.'|| key,label from box.export_header_i18n;

drop table box.export_header_i18n;