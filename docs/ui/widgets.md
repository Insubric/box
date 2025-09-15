---
title: Widgets
permalink: /ui/widgets
parent: UI
nav_order: 4
has_children: true
layout: default
---



# Widget 

Widgets define how a **field** is displayed and edited inside a form.
Not all widgets are valid for every field: **the available widgets depend on the field’s data type** (e.g. numbers, text, dates, child entities).

---

## Rules

* Each field type (e.g. `STRING`, `NUMBER`, `BOOLEAN`) supports a **subset of widgets**.
* Choosing the correct widget ensures proper validation, user experience, and data consistency.
* Some widgets are generic (`input`, `hidden`), while others are specialised (`map`, `richTextEditor`, `simpleChild`).

---

## Categories of Widgets

### 1. **Input Widgets**

Simple editors for direct values.

* `input`
* `textarea`
* `twoLines`
* `integerDecimal2`
* `uuid`
* `inputDisabled`

### 2. **Selection Widgets**

Allow the user to pick from predefined or dynamic options.

* `select`
* `radio`
* `checkbox`
* `tristateCheckbox`
* `selectBoolean`
* `multi`
* `multipleLookup`
* `twoList`
* `popup`
* `dropdownLangWidget`
* `langWidget`

### 3. **Text Editing Widgets**

Rich or code-based editing.

* `richTextEditor`
* `richTextEditorFull`
* `richTextEditorPopup`
* `code`

### 4. **Date & Time Widgets**

Pickers for temporal values.

* `datepicker`
* `datetimePicker`
* `datetimetzPicker`
* `timepicker`

### 5. **Child Widgets**

Used for nested forms and file management.

* `simpleChild`
* `tableChild`
* `editableTable`
* `linkedForm`
* `lookupForm`
* `trasparentChild`
* `spreadsheet`

### 6. **File Widgets**
* `simpleFile`

### 7. **Array Widgets**

Editors for multiple values.

* `inputMultipleText`
* `multi`
* `multipleLookup`
* `twoList`

### 8. **Geometry Widgets**

Spatial editors for geodata.

* `map`
* `mapPoint`
* `mapList`

### 9. **Static Widgets**

Non-editable display elements.

* `staticText`
* `h1`, `h2`, `h3`, `h4`, `h5`
* `lookupLabel`
* `html`

### 10. **Advanced & Utility Widgets**

Special-purpose widgets.

* `dynamicWidget` (switch widget at runtime)
* `popupWidget` (render another widget in a modal)
* `executeFunction` (trigger backend function)
* `hidden` (store values without display)


