---
title: Box
permalink: /
nav_order: 1
---

# BOX

[![Maven central](https://flat.badgen.net/maven/v/maven-central/com.boxframework/box-server_2.12)](https://maven-badges.herokuapp.com/maven-central/com.boxframework/box-server_2.12)


[![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/com.boxframework/box-server_2.12.svg?server=https%3A%2F%2Fs01.oss.sonatype.org&style=flat-square&logo=scala&label=last-snapshot)](https://s01.oss.sonatype.org/content/repositories/snapshots/com/boxframework/box-server_2.12/)


## Supported by


![WSL](https://dms-media.wavein.ch/wi-dms/wsl_small.png)
![wavein.ch](https://dms-media.wavein.ch/wi-dms/wavein_logo_small.png)





## What is BOX
BOX is a framework that aims allowing also non developers to quickly create and modify complete and complex web applications on top of postgres databases, minimizing development and management costs.

This open source project is the result of a fruitful collaboration between the Swiss federal institute of forest, snow and landscape research WSL ([www.wsl.ch](https://www.wsl.ch)) and Wavein.ch Sagl ([wavein.ch](https://wavein.ch)). It has originally been developed to allow environmental researchers with basic database skills to manage their own research data, building web applications on top of it to access and modify data, visualize or export results.

It is now becoming more and more feature rich and is moving outside the academic world, finding application in many other fields like logistics, event organization, content management system (CMS), customer relationship manager (CRM) and others.


BOX is a software divided into two pieces, UI (user interface) and server, that lays on top of PostgreSQL. The UI can run in any modern browser (Chrome, Firefox, Edge, Safari, IE11) and in mobile too. The server run on the JVM (Java Virtual Machine) so it can be run should run in any platform (Linux, Windows, macOS) but tests are done only in Linux


## Why BOX

In many academic research fields there is an increasing need for a well structured and consistent data storage. However having a continuous access to a support from IT professionals may be problematic in small scientific teams, and time and money consuming. Moreover the high customization needs that pops up as the research project evolve, makes often the developed solutions obsolete, waiting for the software developers to implement them. Additionally changes in the software team can quickly make things becoming a nightmare ... Just try to imagine this scenario, which may occur in research much more often than you may expect: you have a database where your team can store field survey data directly from outdoor through the mobile. As you research progress you realize you need a new variable to be measured ... that means a new field in the database, a modification in the ORM and the web application, a deployment on the server ... and the developer of your application just stopped working last week for the software company ...

It is a fact that scientific staff became in the past decades more and more skilled in organizing independently their own research data into databases, and the broad availability of instruments like MSAccess or FileMaker has surely boosted this process. However bringing this data to the web or on mobile devices needs another important step in knowledge acquisition. And if the concepts to manage relational databases are quite stable, web development technologies are changing at a dramatic speed and applications becomes obsolete very quickly, so that keeping up with the technological developments becomes hardly possible for non-IT professionals.

At the Insubric ecosystems research group of WSL we recognized this bottleneck and decided to give more power int he hands of skilled researchers for developing web applications on top of databases. So togeher with wavein.ch and based our common expertise in postgres and scala, which are both open and very powerful tools, we started to imagine a framework that could solve those problems, aiming at a portable solution that could minimize development and management costs.  So back in 2015, when building the application for the Swiss national forest fire database Swissfire we decided to conceive it in a flexible and modular way ... laying the foundations for BOX.

Since then it has been a growing project with many new features, like the management of geographic features or the ability to send emails from the database, and BOX is now used in several academic projects and other non-academic applications.

And many new features are on the way ...

## Features

* Web based UI
* Exploit the features and the knowledge of PostgreSQL
    * Robust and complete SQL DBMS
    * Open source
    * Row level access, every user can access only to the row of the tables that he is allowed 
* Automatic mask generation for tables and views
* Custom database masks generation with support of child tables
* Data exports in CSV and XLS
* Reports in HTML and PDF
* Multi language support
* Role management, customize user interface for each role
* Import data from external systems
* API for integration with other system
* Mobile support
* File support, with image processing capabilities
* Native geographic support (PostGIS)
* Sending mails from the system

