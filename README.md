The main purpose of the exercise was not implement the micro-service in production, however if we would move the following code into production we should implement some considerations in order to be compliant:

## Persistence in Database

For this purpose we are implementing the persistence in memory. For the real scenario and be able to deploy transfer service on production we should need add a real persistence on Database.

Adding persistence into Database, will add extra complexity at whole system, and should be considered implement the following  points in order to accredit consistency system.

#### Encrypt and secure the transfers

Data transfer between database and API should be secured.

## Authorization and Authentication

We should implement a service to Authenticate and Authorize a user in order to be sure that only the valid users can use our transfer system.
For this purpose, we could use JWT and create a security library and using interceptors inject before each call.

## Audit

For this exercise we create a Transfer repository in memory as way to audit and store the transfers that the users are performing.

In the real scenario this repository as the same case than accounts should be stored into Database. Transfer service could write into a queue all the events that transfer is performing.
Another service should treat theses events and check the consistency of the transfers.

## Containers and Continuous integration / delivery pipeline

A continuous integration / delivery pipeline should be used to ensure code quality and continuous improvement of the base code.

#### Execution pipeline

Execution pipeline (jenkins, bamboo, etc...) should be necessary for execute all the task that CI include, execution pipeline needs guarantee quality of the code.

#### Container system

A container system (docker) could be implemented to easy deployment and scalability of services.

#### Code Analysis Tool

Code analysis tool (Sonar) must be needed to ensure the correct quality of the code.