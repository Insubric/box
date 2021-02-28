---
title: Getting started
permalink: /getting_started/
nav_order: 2
---

# Getting started

To start a new project with box framework simply do:
```
sbt new minettiandrea/box.g8
```

Provide the data required, then go to the project folder:
```
cd <project-name>
```

Install the box schema
```
sbt installBox
```

And generate your model
```
sbt generateModel
```

Your application is ready!

Run it locally:
```
sbt run
```
or check our [deploy options](/deploy/)