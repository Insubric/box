---
title: Interface builder
permalink: /ui/interface-builder
parent: UI
nav_order: 4
has_children: true
layout: default
---

# Interface builder

- [Base Info](#base-info)
- [Table Info](#table-info)
- [Fields](#fields)
- [Linked forms](#linked-forms)
- [Static elements](#static-elements)
- [Actions](#actions)

# Form – Interface Builder Documentation

The **Interface Builder** in the Box Framework provides a visual editor for creating and managing forms bound to database entities. It allows defining fields, layouts, linked forms, and static elements without writing code.

---

## Base Info

The **Base Info** section defines the core identity and structure of the form.

* **name (required):**
  Internal identifier of the form. Example: `places`.

* **entity (required):**
  Database entity/table this form maps to. Example: `places`.

* **Key fields:**
  By default, the entity’s primary key is used. Can be customised if the entity does not have a primary key (i.e. views).

* **query:**
  Optional filter expression for pre-loading data into the form.

* **Layout:**
  Controls the visual arrangement of fields. Options:

    * `Form layout` (default)
    * Other custom layouts.

---

### Internationalisation (I18n)

Define multilingual labels for the form:

* **lang:** Language code (`en`, `it`, etc.).
* **label:** Human-readable name in that language. Example: `places`.
* **view\_table:** Specifies the table/view to use for the translation.

---

## Table Info

Specifies the fields visible in tabular (list) views and export functionality.

* **tabularFields (required):**
  Comma-separated list of fields shown in table view.
  Example: `id, region_id, name, description, location…`.

* **exportFields:**
  Fields included when exporting data from the form.
  Example: `id, region_id, name, description, location…`.

---

## Fields

Defines the form fields, their widgets, and behaviour.

| **Attribute**       | **Type / Input**     | **Required** | **Description**                                                                                                             | **Example**                                                  |
| ------------------- | -------------------- | ------------ |-----------------------------------------------------------------------------------------------------------------------------| ------------------------------------------------------------ |
| **name**            | Text                 | ✅ Yes        | Internal identifier for the field. Must be unique within the form.                                                          | `accuracy_meters`                                            |
| **type**            | Dropdown             | ✅ Yes        | Data type stored in the database.                                                                                           | `string`, `number`, `boolean`, `date`, `datetime`, `json`    |
| **widget**          | Dropdown             | ✅ Yes        | UI control used for editing/displaying the field. See [widget](/ui/widget) section for a complete list of available widgets | `input`, `selectWidget`, `map`, `datetimePicker`, `checkbox` |
| **required**        | Checkbox             | No           | If checked, field must be filled before saving.                                                                             | ✔ Required                                                   |
| **read\_only**      | Checkbox             | No           | If checked, field is visible but cannot be edited.                                                                          | ✔ Read-only                                                  |
| **default**         | Text / Value         | No           | Default value applied when creating a new record.                                                                           | `0`, `Unknown`                                               |
| **min**             | Number               | No           | Minimum allowed value (for numeric fields).                                                                                 | `0`                                                          |
| **max**             | Number               | No           | Maximum allowed value (for numeric fields).                                                                                 | `1000`                                                       |
| **condition**       | Expression           | No           | Conditional logic controlling visibility or editability.                                                                    | `show if location != null`                                   |
| **params**          | JSON / Key-Value     | No           | Widget-specific configuration (e.g., step size, placeholder).                                                               | `{ "step": 0.1, "placeholder": "Enter meters" }`             |
| **roles**           | List (text)          | No           | Restrict field access to certain user roles. If empty, defaults apply.                                                      | `["admin", "editor"]`                                        |
| **foreign\_entity** | Dropdown / Reference | No           | Links the field to another entity (used for relations).                                                                     | `regions`                                                    |

---

### I18n Field Attributes (Right Panel)

Each field supports **internationalisation** for multilingual forms. For each language (`field_i18n` entry), you can configure:

| **I18n Attribute**      | **Type / Input** | **Required** | **Description**                                                                              | **Example**                                 |
| ----------------------- | ---------------- | ------------ | -------------------------------------------------------------------------------------------- | ------------------------------------------- |
| **lang**                | Dropdown (code)  | ✅ Yes        | Language code for this translation.                                                          | `en`, `it`, `fr`                            |
| **label**               | Text             | No           | User-facing label for the field in this language.                                            | `Accuracy (meters)`                         |
| **Foreign label field** | Text / Reference | No           | If the field points to a foreign entity, defines which column from that entity is displayed. | `region_name`                               |
| **placeholder**         | Text             | No           | Text shown inside the input field when empty.                                                | `Enter accuracy`                            |
| **tooltip**             | Text             | No           | Help text shown on hover.                                                                    | `Specify GPS accuracy, in meters (0–1000).` |

You can add multiple translations by clicking **➕ field\_i18n**.

---


## Linked Forms

The **Linked forms** section allows you to associate a **child form** with the current (parent) form. This is useful for managing **one-to-many relationships** (e.g., a place having multiple images).

---

### Core Attributes (Left Panel)

| **Attribute**         | **Type / Input**     | **Required** | **Description**                                                                 | **Example**              |
| --------------------- | -------------------- | ------------ | ------------------------------------------------------------------------------- | ------------------------ |
| **name**              | Text                 | ✅ Yes        | Internal identifier for the linked form. Must be unique within the parent form. | `child_images`           |
| **widget**            | Dropdown             | ✅ Yes        | Widget type for rendering the linked form. Common option: `simpleChild`.        | `simpleChild`            |
| **read\_only**        | Checkbox             | No           | If enabled, the child form is visible but cannot be edited.                     | ✔ Read-only              |
| **Child form**        | Dropdown / Reference | ✅ Yes        | The form that will be embedded as a child.                                      | `places_images`          |
| **Open child form**   | Checkbox / Action    | No           | Allows direct opening of the child form in a separate view.                     | –                        |
| **Parent key fields** | List (required)      | ✅ Yes        | Fields in the parent form that serve as keys in the relationship.               | `id`                     |
| **Child key fields**  | List (required)      | ✅ Yes        | Fields in the child form that link back to the parent.                          | `id`                     |
| **childQuery**        | Expression           | No           | Optional query/filter for limiting which child records are shown.               | `status = 'active'`      |
| **condition**         | Expression           | No           | Controls whether the linked form is displayed.                                  | `show if type = 'image'` |
| **params**            | JSON / Key-Value     | No           | Extra widget configuration (layout, behaviour, etc.).                           | `{ "maxItems": 10 }`     |
| **roles**             | List (text)          | No           | Restrict access to certain user roles.                                          | `["admin", "editor"]`    |

---

### I18n Attributes (Right Panel)

Just like fields, linked forms can be **internationalised**:

| **I18n Attribute** | **Type / Input** | **Required** | **Description**                           | **Example**                                |
| ------------------ | ---------------- | ------------ | ----------------------------------------- | ------------------------------------------ |
| **lang**           | Dropdown (code)  | ✅ Yes        | Language code.                            | `en`, `it`, `fr`                           |
| **label**          | Text             | No           | Human-readable label for the linked form. | `Images`                                   |
| **tooltip**        | Text             | No           | Hover help text for the linked form.      | `Upload and manage images for this place.` |

Multiple translations can be added via **➕ field\_i18n**.



**Use Case:**
Attach related forms (e.g., uploading images for each place).

---

## Static Elements

Static (non-editable) UI elements in the form.

| **Name**  | **Widget** | **Read Only** | **Foreign Entity** |
| --------- | ---------- | ------------- | ------------------ |
| (example) | –          | –             | –                  |

---

## Actions

Toolbar options at the top of the interface:

* **Save** – Persist changes.
* **New** – Create a new form definition.
* **Copy** – Duplicate current form setup.
* **Delete** – Remove form definition.
* **Revert** – Undo unsaved changes.
* **Go to form** – Open the live form preview.

