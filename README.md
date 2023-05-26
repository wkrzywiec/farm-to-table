# Food Delivery
[![Build](https://github.com/wkrzywiec/food-delivery-app/actions/workflows/build.yaml/badge.svg?branch=main)](https://github.com/wkrzywiec/food-delivery-app/actions/workflows/build.yaml) ![GitHub](https://img.shields.io/github/license/wkrzywiec/food-delivery-app) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=wkrzywiec_food-delivery-app&metric=coverage)](https://sonarcloud.io/summary/new_code?id=wkrzywiec_food-delivery-app) [![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=wkrzywiec_food-delivery-app&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=wkrzywiec_food-delivery-app) ![GitHub issues](https://img.shields.io/github/issues/wkrzywiec/food-delivery-app)

This is a sandbox application for me to try out new concepts and technologies.

## Usage

### With Taskfile
The easiest way to start working with a project is to run one of the tasks from the Taskfile:

```bash
task -l
```

Exemplary output:

```
* check:        Verify local tools
* dev:backend:  Run backend apps with gradle
* infra:        Spin up dockerized infrastructure
* infra:clean:  Stop and clean all persisted data
* infra:down:   Stop infrastructure
* init:         Run all services in Docker & add initial data
* run:          Run all services in Docker
```

In example, in order to run all services with initial data, run the command:

```bash
task init
```

### With `docker compose`

If you prefer not to install or use Taskfile, you can easily run all services with a simple command:
```bash
docker compose up -d
```

To explore the additional commands available in the Taskfile, please refer to the `Taskfile.yaml` file.

## Requirements

o run all services on your local machine, you will need to have Docker installed.

To develop either a backend or frontend service, you will need the following software installed:

* Java
* npm and Node
* Taskfile

Please note that specific versions of each software can be found within each service.

## Configuration

## Project description

The Food Delivery app is designed as a microservice system, aiming to replicate real-world solutions in a simplified manner.

As this project serves as my sandbox for experimenting with new concepts and technologies, it will continue to evolve over time. Initially, it primarily functions as an event-driven system, with services communicating with each other based on events.

### C4 model

```mermaid
C4Container
    title Container diagram for Food Delivery
    
    System_Ext(email_system, "E-Mail System", "The internal Microsoft Exchange system", $tags="v1.0")
    Person(customer, Customer, "A customer of the bank, with personal bank accounts", $tags="v1.0")
    
    Container_Boundary(c1, "Internet Banking") {
        Container(spa, "Single-Page App", "JavaScript, Angular", "Provides all the Internet banking functionality to cutomers via their web browser")
        Container_Ext(mobile_app, "Mobile App", "C#, Xamarin", "Provides a limited subset of the Internet banking functionality to customers via their mobile device")
        Container(web_app, "Web Application", "Java, Spring MVC", "Delivers the static content and the Internet banking SPA")
        ContainerDb(database, "Database", "SQL Database", "Stores user registration information, hashed auth credentials, access logs, etc.")
        ContainerDb_Ext(backend_api, "API Application", "Java, Docker Container", "Provides Internet banking functionality via API")
    
    }
    
    System_Ext(banking_system, "Mainframe Banking System", "Stores all of the core banking information about customers, accounts, transactions, etc.")
    
    Rel(customer, web_app, "Uses", "HTTPS")
    UpdateRelStyle(customer, web_app, $offsetY="60", $offsetX="90")
    Rel(customer, spa, "Uses", "HTTPS")
    UpdateRelStyle(customer, spa, $offsetY="-40")
    Rel(customer, mobile_app, "Uses")
    UpdateRelStyle(customer, mobile_app, $offsetY="-30")
    
    Rel(web_app, spa, "Delivers")
    UpdateRelStyle(web_app, spa, $offsetX="130")
    Rel(spa, backend_api, "Uses", "async, JSON/HTTPS")
    Rel(mobile_app, backend_api, "Uses", "async, JSON/HTTPS")
    Rel_Back(database, backend_api, "Reads from and writes to", "sync, JDBC")
    
    Rel(email_system, customer, "Sends e-mails to")
    UpdateRelStyle(email_system, customer, $offsetX="-45")
    Rel(backend_api, email_system, "Sends e-mails using", "sync, SMTP")
    UpdateRelStyle(backend_api, email_system, $offsetY="-60")
    Rel(backend_api, banking_system, "Uses", "sync/async, XML/HTTPS")
    UpdateRelStyle(backend_api, banking_system, $offsetY="-50", $offsetX="-140")
```
