
Login to the db docker:
```
docker exec -it bd573be110c4 /bin/bash
```

Then listen for the stream
```
pg_recvlogical -d postgres --slot test_slot --create-slot -P wal2json --dbname wss_wsu -U postgres
pg_recvlogical -d postgres --slot test_slot --start -o pretty-print=1 -o include-xids=1 -o include-timestamp=1 -o add-msg-prefixes=wal2json -f - --dbname wss_wsu -U postgres
```