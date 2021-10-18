# AWS provisioning: Java project template

This project is a POC. The goal is to use Temporal to provision Temporal on AWS.

## Status
The project is creating and deleting an S3 bucket.

## Build the project

Open the project in IntelliJ, which will automatically build it

## Run the Workflow

First, make sure the [Temporal server](https://docs.temporal.io/docs/server/quick-install) is running.

To start the Workflow, run the InitiateAws class from IntelliJ.

To start the Worker, run the AwsWorker class from IntelliJ.

To submit the signal, run the SignalAws class from IntelliJ.