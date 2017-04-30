# PROJECT README

## Introduction

The purpose of this project is to create a software testbed of various implementations with the same initial requirements in order to research how certain decisions and metrics affect system agility and the amount of effort that must be invested to change a system from its initial state. This is one such implementation.

## Implementation

### Functionality
All required object models (customer accounts, bank accounts, debit cards) are annotated and made persistent via JPA/Hibernate. The implementation thus far provides functionality for creating new customer accounts, bank accounts and debit cards, making physical deposits of money to bank accounts, transferring money between bank accounts, viewing transaction history and paying via debit cards at PIN machines. Debit cards can expire and bank accounts can be closed.

### General Architecture
All relevant data is contained in the CustomerAccount, BankAccount, DebitCard and Transaction objects. Each CustomerAccount can have multiple BankAccounts, each BankAccount can have multiple DebitCards and Transactions. 

A DataManager object handles the saving/updating/removal of the aforementioned objects, using the SQLiteDB object to initialize/connect to the local SQLite database.

The BankingServer, InputChecker and Session objects in the server package are used to keep track of the user's actions and process their input.

The TUI object is used for prompts/display to the user, and the Client object is the point of entry for the program.

## Dependencies

* Hibernate ORM -- For easy storage of Java objects in a relational database
* Hibernate SQLite dialect -- For SQLite compatibility with Hibernate
* SL4J-Log4J -- For dumping most log messages to file
* SQLite-JDBC -- Java database connectivity for SQLite
* commons-validator -- For validating various forms of user input
* JSONRPC2-base -- For (future) JSON messaging protocol

All of the aforementioned dependencies are automatically managed via Maven.

## Significant Assumptions

* That a proper server-client messaging protocol is not yet necessary.
* That the application will not be network-based in the near future.
* That it is not yet necessary for a customer to manage the privileges of other customers over his/her bank account, despite the fact that the functionality exists.
* That administrative database management/modification can be handled outside of the program, and that there is no need for such functionality within the program.

## Instructions

Locate the Client class in the client package and run it as a Java application. All input/output is currently console-based. Current command syntax is INSTRUCTION [parameter1]:[parameter2]:[parameter...].
