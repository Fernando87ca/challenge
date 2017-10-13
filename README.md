The main purpose of the exercise was not implement the micro-service in production, however if we would move the following code into production we should implement some considerations in order to be compliant:

## Accounts should be allocated in a Real Database, not in memory:

In the real scenario there is not enough having the accounts in memory, we should need a real database to store the Account information, with this new premise we could needed:

* Authorization and Authentication services.
* Communication between database and micro-service should be secure.
* it would be necessary to add Audit, in order to keep track all the user movements.
* RxJava Observables in order to save the transfers in asynchronous way.


## Transfers Repository:

To implement the api in production we will need a Transfer repository, another micro-service could read from transaction repository at the end of day and check if all the accounts are in a consistance status, to implement this, we should need on this micro-service the following considerations:

* Log transfer store.
* Notification process in queue after each success transfer

## Continuous Integration process:


As Micro-service architecture, we should need implement a continuous integration pipeline in order to provide new functionality, quick fix or new versions of the diferents micro-services that we are generating
