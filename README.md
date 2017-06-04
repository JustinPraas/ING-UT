# PROJECT README

## Introduction

The purpose of this project is to create a software testbed of various implementations with the same initial requirements in order to research how certain decisions and metrics affect system agility and the amount of effort that must be invested to change a system from its initial state. This is one such implementation.

## Implementation

### Functionality
All required object models (customer accounts, bank accounts, debit cards, transactions) are annotated and made persistent via JPA/Hibernate. The implementation thus far provides functionality for creating new customer accounts, bank accounts and debit cards, making physical deposits of money to bank accounts, transferring money between bank accounts, viewing transaction history and making payments via debit cards. Debit cards can expire and bank accounts can be closed. Customers can have access given and revoked to/from other customers' bank accounts.

### General Architecture
All relevant data is modeled via the CustomerAccount, BankAccount, DebitCard and Transaction objects. Each CustomerAccount can have multiple BankAccounts, each BankAccount can have multiple DebitCards and Transactions. 

The DataManager object handles the saving/updating/removal of the aforementioned objects, using the SQLiteDB object to initialize/connect to the local SQLite database.

On the server, the ClientHandler object handles all serverside processing of user input, along with replying to user requests. It is implemented as a RESTful service using Jersey. The InputChecker object is used to verify that the values of the provided JSON-RPC parameters are valid. 

There is also a client implementation for quick functionality testing. This is implemented through the Client, TUI and MessageHandler objects. The Client object creates a TUI, which opens a MessageHandler instance. The TUI handles display to the user and listens for messages. The MessageHandler translates valid user input into JSON-RPC requests, which it sends to the server. MessageHandler also interprets messages from the server and displays their results in a human-readable form to the user.

## Dependencies

* Hibernate ORM -- For easy storage of Java objects in a relational database
* Hibernate SQLite dialect -- For SQLite compatibility with Hibernate
* SL4J-Log4J -- For dumping most log messages to file
* SQLite-JDBC -- Java database connectivity for SQLite
* commons-validator -- For validating various forms of user input
* JSONRPC2-base -- For the implementation of the JSON-RPC messaging protocol
* Apache HttpClient5 -- For HTTP messaging between client and server
* Jersey -- For the implementation of RESTful services

All of the aforementioned dependencies are automatically managed via Maven.

## Significant Assumptions

* That having matching IDs for corresponding JSON-RPC request/response pairs is currently irrelevant
* That security is completely outside the scope of this project
* That we are not considering the possibility of having multiple concurrent clients
* That transaction atomicity is not an implicit part of the given requirements specification

## Instructions

Place the bankingtables file on your desktop. This is also where the banking.db file will be created.

Download Tomcat 8.5, set up a Tomcat server in Eclipse JEE. Via the Server view, add the ING-UT project to the server and start the server. 

Once the server is started, locate the Client class in the client package and run it as a Java application. All input/output is currently console-based. The command syntax is COMMAND [parameter1]:[parameter2]:[parameter...].
