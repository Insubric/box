---
title: Ui Notification on websocket channel
parent: Documentation
nav_order: 2
---

## Postgres methods

- `box.ui_notification_forall(topic text, payload json)` sends out the notification to all connected clients
- `box.ui_notification(topic text, users text[], payload json)` send the notification only to the users in the users array

## Notification in UI
A notification channel is automatically set up, in order to send messages from the db to the ui use the provided `box.ui_notification` or `box.ui_notification_forall` function in postgres.

Example:

SQL
```
SELECT box.ui_notification_forall('box-client', '{"body": "Notification test"}')
```

A common usage is to include the method call in a trigger, please note that for that case you may want to use `PERFORM` instead of `SELECT`, moreover the user executing the trigger must have access to box schema, that in not usually the case, a way to overcome to that limitation is to declare the trigger function as with `SECURITY DEFINER` that way the trigger is executed with the privileges of the creator of the trigger, commonly the administrator. 

## Websocket channel
It may be useful to exploit the websocket channel for other applications than the notification on the box-ui client (i.e. another client using box as backend), a client may subscribe to a topic by opening a websocket on the following path:

`/api/v1/notifications/<topic name>`

where topic name is the first parameter of the postgres defined functions.

## Tech notes
The websocket relay on the LISTEN/NOTIFY functionality of PostgreSQL [https://www.postgresql.org/docs/9.1/sql-listen.html](https://www.postgresql.org/docs/9.1/sql-listen.html)
Unfortunately the JDBC implementation of this is partial so the LISTEN functionality is implemented as long polling, instead of an ideal reactive implementation as suggested by the Postgres JDBC documentation [https://jdbc.postgresql.org/documentation/94/listennotify.html](https://jdbc.postgresql.org/documentation/94/listennotify.html)