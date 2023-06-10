# Food Delivery
[![Build](https://github.com/wkrzywiec/food-delivery-app/actions/workflows/build.yaml/badge.svg?branch=main)](https://github.com/wkrzywiec/food-delivery-app/actions/workflows/build.yaml) ![GitHub](https://img.shields.io/github/license/wkrzywiec/food-delivery-app) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=wkrzywiec_food-delivery-app&metric=coverage)](https://sonarcloud.io/summary/new_code?id=wkrzywiec_food-delivery-app) [![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=wkrzywiec_food-delivery-app&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=wkrzywiec_food-delivery-app) ![GitHub issues](https://img.shields.io/github/issues/wkrzywiec/food-delivery-app)

This application is a simplified food delivery platform, similar to Uber Eats or Glovo. It serves as a sandbox environment for experimenting with novel concepts and technologies.

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
* dev:bff:      Run 'bff' service
* dev:delivery: Run 'delivery' service
* dev:food:     Run 'food' service
* dev:ordering: Run 'ordering' service
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

To run all services on your local machine, you will need to have Docker installed.

To develop either a backend or frontend service, you will need the following software installed:

* Java
* npm and Node
* Taskfile

Please note that specific versions of each software can be found within each service.

## Project description

The Food Delivery app is designed as a microservice system, aiming to replicate real-world solutions in a simplified manner.

As this project serves as my sandbox for experimenting with new concepts and technologies, it will continue to evolve over time. Initially, it primarily functions as an event-driven system, with services communicating with each other based on events.

### Services

The entire system is made of 5 microservices (1 React, 4 Java/Spring):

* *ui* - React application, used by customers to place orders, manage them and track deliveries, url: http://localhost:80,
* *bff* - backend for frontend service, used to provide REST endpoint for *ui*, url: http://localhost:8081/swagger-ui.html,
* *food* - service that handles adding available meals to Redis, url: http://localhost:8084/swagger-ui.html,
* *ordering* - core service for managing orders,
* *delivery* - core service for managing deliveries.

### C4 diagrams

#### C4 container diagram

The "Configuration" section highlights that the backend services can be customized to utilize various components such as databases, queues, and more. As a result, it becomes challenging to have a single unified C4 container diagram that represents every profile. However, provided below is a default profile diagram for reference.

![c4-container](/docs/c4-model-Containers.png)

## Configuration

The majority of the backend application is designed to be configurable, allowing to specify the infrastructure components it can utilize for various tasks such as data persistence and message queuing during startup.

Currently, there is only one available profile called `redis`. However, there are plans to introduce additional profiles in the near future.

### Default configuration (`SOON`)

### `redis` configuration

All of the infrastructure components are utilizing Redis. This profile serves as a showcase to demonstrate the versatility of Redis and its ability to support various functionalities across the application.
Run it with a task:

```bash
task run
```

Here is the overview of a system architecture with used Redis modules:

![c4-container](/docs/redis/architecture.png)

To check all data stored in Redis enter in a web browser: http://localhost:8181


#### How it works

Most of the communication is based on commands and events. E.g. in order to place an order a proper command needs to be pushed to `orders` Redis Stream. It's then read and processed by the `ordering` service and result in an event which also added to the central `orders` stream, so other services, like `delivery` can read and process further.

Also `bff` is reading from the `orders` stream to create its own read model (Command Query Responsibility Segregation, CQRS) of all deliveries and store it in Redis Hash. These are used to serve a current state of a delivery on a frontend.

Both `ordering` and `delivery` services have their own event stores in which they store relevant events, so they can event source them to make a projection of an order or a delivery.

All requests that are coming from a frontend are first queued in two Redis Task queues - `ordering-inbox` and `delivery-inbox`. These inboxes are used to store all incoming REST requests to `bff` before converting them to relevant commands and publishing to the `orders` stream.

Finally, the `food` service stores all available meals in the `food` RedisJSON store. It also has an index created which enables a full-text search of all meals on a frontend.

#### How the data is stored:

There are several Redis modules that are used to store data:

* `orders` - Redis Stream, used to store commands and events as JSON. It stores all events that are happening across the entire system. `bff` is publishing commands into it. `ordering` & `delivery` are publishing events. Exemplary event:
```json
{
   "header": {
      "messageId":"c065e910-1806-4ab5-b1c9-8c8a105323f6",
      "channel":"orders",
      "type":"FoodDelivered",
      "itemId":"order-2",
      "createdAt":"2022-08-28T12:14:10.557171900Z"
   },
   "body":{
      "orderId":"order-2"
   }
}
```
![orders stream](/docs/redis/orders-stream.png)

* `ordering::[orderId]` & `delivery::[orderId]` - Redis Streams, used to store only events relevant events for each service as JSON. Each order/delivery has its own stream. They hold the same events as it's in the `orders` stream.

![delivery stream](/docs/redis/delivery-stream.png)

* `delivery-view` - Redis Hash, used to store delivery read models used for a frontend. `field` in the hash stores an orderId and `value` stores a projection of a delivery. Data is populated here by the `bff` service. Exemplary delivery view:

```json
{
   "orderId":"order-2",
   "customerId":"Pam Beesly",
   "restaurantId":"Ristorante Da Aldo",
   "deliveryManId":"nicest-guy",
   "status":"FOOD_DELIVERED",
   "address":"Cottage Avenue 11",
   "items":[
      {
         "name":"Lemony Asparagus Penne",
         "amount":1,
         "pricePerItem":9.49
      },{
         "name":"Tea",
         "amount":1,
         "pricePerItem":1.99
      }
   ],
   "deliveryCharge":1.99,
   "tip":4.59,
   "total":18.06
}
```

![delivery view](/docs/redis/delivery-view.png)

* `__rq` - Redis task queue, two inboxes (`ordering-inbox` & `delivery-inbox`) used to store incoming REST request and queue their processing. Their entire lifecycle is managed by the library [sonus21/rqueue](https://github.com/sonus21/rqueue). Data is here populated and consumed by the `bff` service.

![redis task queue](/docs/redis/rqueue.png)

* `food:[foodId]` - RedisJSON document, stores information about meals. Data are populated by the `food` service.

![food json](/docs/redis/food-json.png)

* `food-idx` - RedisJSON index, index for `food:[foodId]` RedisJson document used to enable full-text search of meal name.

![food idx](/docs/redis/food-idx.png)

#### How the data is accessed:

* `orders` - `bff`, `ordering` & `delivery` are consumers of this stream. They're using standard Spring Boot `StreamListener`.
* `ordering::[orderId]` & `delivery::[orderId]` - `ordering` & `delivery` services are the producers and consumers for these event stores. They're using the Spring Boot `RedisTemplate` to achieve it.
* `delivery-view` - `bff` is adding and fetching data from this hash using the Spring Boot `RedisTemplate`.
* `__rq` - `bff` is queueing and consuming data from the task queue using the [sonus21/rqueue](https://github.com/sonus21/rqueue) library.
* `food:[foodId]` & `food-idx` - `bff` is using the RedisLab's `StatefulRediSearchConnection` to full-text search available meals.



