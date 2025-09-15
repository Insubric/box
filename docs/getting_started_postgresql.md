---
title: PostgreSQL Template
parent: Getting started
permalink: /getting_started/postgresql
nav_order: 2
layout: default
---

## Setting Up Your PostgreSQL Database

1. **Install PostgreSQL**:
    - Ensure that PostgreSQL is installed on your system. You can download and install it from the [official PostgreSQL website](https://www.postgresql.org/download/).

2. **Create a New Database**:
    - Open your terminal or command prompt and connect to the PostgreSQL server using the `psql` command-line tool.
    - Create a new database by running the following command:
      ```sql
      CREATE DATABASE mydatabase;
      ```
    - Replace `mydatabase` with your desired database name.

3. **Use a Template**:
    - To simplify the setup process, you can use one of our provided templates. These templates include a predefined schema and sample tables.
    - Download the template SQL file from our [templates repository](https://github.com/Insubric/box-starter/tree/main/sql_templates).
    - Open psql
      ```bash
      psql -h <database-host> -U <database-user> -p <database-port:default 5432> mydatabase
      ``` 
    - Import the template into your new database by running:
      ```sql
      \i /path/to/template.sql
      ```
    - Replace `/path/to/template.sql` with the actual path to the downloaded template file.
