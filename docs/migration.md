---
title: Migration
permalink: /migration/
nav_order: 6
---

# Migrate 1.2.x to 1.3.x

Box 1.3.x brings many improvements to the box admin page and on the form flexibility.
A new kind, of form is defined, pages, that are static form (without underlying table), that are useful to present and create workflows.

## Lang
In order to manage the lang in the all application stack (in particular starting from codegen level), the enabled lang definition is moved to the application.conf file.
Therefore you need to add the lang on the application.conf as a comma separated list
```
langs = "en,it,de"
```
