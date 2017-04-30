# PROJECT README

## Introduction

The purpose of this project is to create a software testbed of various implementations with the same initial requirements in order to research how certain decisions and metrics affect system agility and the amount of effort that must be invested to change a system from its initial state. This is one such implementation.

## Implementation

## Dependencies

* Hibernate ORM -- For easy storage of Java objects in a relational database
* Hibernate SQLite dialect -- For SQLite compatibility with Hibernate
* SL4J-Log4J -- For dumping most log messages to file
* SQLite-JDBC -- Java database connectivity for SQLite
* commons-validator -- For validating various forms of user input
* JSONRPC2-base -- For (future) JSON messaging protocol

All of the aforementioned dependencies are automatically managed via Maven.

## Assumptions

## Instructions

Locate the Client class in the client package and run it as a Java application. All input/output is currently console-based. Current command syntax is INSTRUCTION [parameter1]:[parameter2]:[parameter...].
