---
title: Deploy
permalink: /deploy/
nav_order: 3
---

# Deploy

## Versioning
Versions are managed using git tags, to create a new version simply add a new tag to your repository using: 
```
 git tag <version>
```
The format of the version is `x.y.z`, i.e. `1.3.5`.

## CI, Github actions
If you created the project using the g8 template github action for publishing your application to github releases and docker hub is already set up, you just need to:
1. create a github repository for the project (we advise to do it private since the information and accesses of your DB are exposed)
2. Add the git hub secrets for docker hub access:
    1. `DOCKERHUB_USERNAME` with your hub.docker.com username
    2. `DOCKERHUB_PASSWORD` with your hub.docker.com access token (https://docs.docker.com/docker-hub/access-tokens/)



## Standalone

Generate the application package with
```
sbt universal:packageBin
```

Standalone package is available in
`server/target/universal/server-1.0.0.zip`

In the zip you will find a `bin/` directory to start the server use: 
```
bin/boot
```
if you want to provide an external configuration file you may use:
```
bin/boot -Dconfig.file=<path of the file>
```

## Cloud foundry 

To deploy to cloud foundry (i.e. Swisscom developer cloud) first package the app with
```
sbt box
```
then upload it with:  
```
cf push -p server/target/universal/server-1.0.0.zip <app-name>
```

Database configuration can be done in `resources/application.conf`, a template of `application.conf` is provided in
`application.template.conf`, we therefore advise to use env variables to setup the database connection

Set Env variables:
- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `DB_SCHEMA`
- `BOX_DB_URL`
- `BOX_DB_USER`
- `BOX_DB_PASSWORD`
- `BOX_DB_SCHEMA`

## Docker

In order to create a docker image of your setup use:
```
sbt docker:publishLocal
```

A local image will be created, to run it you need to have docker installed in your machine (for desktop use https://www.docker.com/products/docker-desktop)

to run Box using docker swarm first create a configuration

```
docker config create box-config <path-to>/application.conf
```

then run box as a service

```
docker service create --config src=box-config,target=/application.conf --publish published=8080,target=8080 --name box <your-image-name>:<your-tag>
```


