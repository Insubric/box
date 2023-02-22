update field set widget='checkbox' where widget = 'checkboxNumber';

update field set params='{"fullWidth": true}',widget='datepicker' where widget='datepickerFullWidth' and params is null;
update field set params='{"fullWidth": true}',widget='timepicker' where widget='timepickerFullWidth' and params is null;
update field set params='{"fullWidth": true}',widget='datetimePicker' where widget='datetimePickerFullWidth' and params is null;
update field set params='{"nolabel": true}',widget=null where widget='nolabel' and params is null;

update field set widget='input' where widget = 'inputNumber';
update field set widget='input' where widget = 'inputArrayNumber';
update field set widget='input' where widget = 'textinput';

update field set widget='map' where widget = 'mapPoint';