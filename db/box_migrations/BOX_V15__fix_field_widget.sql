update field set params='{"fullWidth": true}',widget=null where widget='fullWidth' and params is null;

update field set widget='selectWidget' where widget is null and "lookupEntity" is not null;

update field set "type"='static' where widget like 'title_%' or widget='static_text';