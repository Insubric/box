---
title: Create forms
permalink: /ui/create-forms
parent: UI
nav_order: 2
has_children: true
layout: default
---

# Create forms

To create a new form in the Box Framework, follow these steps:

1. **Login with the Administration User**: Access the administration panel by logging in with your administration credentials (by default the database user you used to setup the project).
1. **Navigate to the Admin Section**: Once logged in, go to the admin section of the application.
1. **Open the Create Form Page**: In the admin section, find and open the "Create Form" page (or directly `<base_url>/admin/form/create` i.e. [http://localhost:8080/admin/form/create](http://localhost:8080/admin/form/create).

![Form create](/assets/images/form_create.png)


When creating a new form, you will encounter several key fields:


- **Name**: This field allows you to specify a unique name for your form. The name should be descriptive and reflect the purpose of the form. For example, "NewObservation".
- **Select Main Table**: This dropdown menu allows you to choose the main table in your database that the form will interact with. The selected table will determine the structure and fields available for the form. For example, selecting the "users" table will allow you to create a form that inserts data into the "users" table.
- **Define Child Tables**: This section allows you to specify child tables that are in a one-to-many relationship with the main table. When you select a main table, you can define related child tables to capture additional data. For example, if your main table is "orders," you might define a child table "order_items" to capture details about each item in an order. This feature enables you to handle complex data structures and relationships within a single form, making data entry more efficient and organized.
- **Add to Homepage**: This checkbox allows you to specify whether the form should be added to the homepage of your application. If checked, the form will be accessible from the main page, making it easily available to users. This is useful for forms that are frequently used, such as login or registration forms.
- **Roles**: This field allows you to specify which user roles have access to the form. You can select multiple roles from a list of available roles. For example, you might allow only "Admin" and "Manager" roles to access a form for updating user permissions. This ensures that only authorized users can interact with the form, enhancing security and data integrity.


After creating, you will be redirected to the tabular view of the form.

## Next Step
If you want to customize your form, you can click on 'Edit UI' to open the [Interface Builder page](/ui/interface-builder).