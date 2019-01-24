## Disclaimer
#### What should have been clarified?
1. UI.
    1. More details about how progress report should look like (kinds of messages, nature - e.g. should it be shown in lines or percents, etc.). I assumed lines and print them upon each processed chunk.
    1. Any interaction with user during execution. For now only '^ + C' is supported :)
1. What settings should we expose and how. Degree of parallelism, size of a chunk, some user input constraints, etc. I assumed certain reasonable defaults and allowed end user to specify parallelism/directories. Other things are either internally configurable or hardcoded.
1. Should directories always reside on the same machine? I assumed 'yes'. If it's not the case - we can always mount a remote directory to a local machine, or, as a last resort, with Spring Batch it shouldn't be a big problem to do tis programmatically: it can be configured to send information to remote nodes, especially with help of Spring Integration.
1. Handling of empty files and prerequisites. Currently I don't allow to specify a source directory, which doesn't have at least one non-empty file. In case there are both empty and non-empy - corresponding CSV for an empty file will be empty as well. Business might think otherwise :)
#### Shortcuts and hacks
There are some, since it's a non-production code with little-to-none non-functional requirements. I've left implementation comments to highlight them.
## Architecture
Is pretty simple. Spring Batch was selected (as a compromise between simplicity, extensibility and performance) as an ETL framework. [Partitioning paradigm](https://docs.spring.io/spring-batch/4.1.x/reference/html/index-single.html#partitioning) appeared to almost perfectly fulfill given use case (ETL-style parallel processing of CSV files). Business logic is encapsulated in custom Item Processor (`com.example.batch.processing.LineProcessorImpl`). Progress reporting (see package `com.example.batch.progress`) implemented using event-driven approach (as recommended by the official documentation).
Basic input parameter validation was also implemented (see `com.example.CliValidation`) and executed before starting of a Spring app. Spring Boot was used as a main "backbone" framework and its autoconfiguration features were heavily utilized in order to reduce codebase. 
Configuration was done in a Java-based way and almost fully encapsulated in `com.example.batch.BatchConfiguration`.
Maven was selected as a build management system as one of the most popular on the market and familiar to me, as a developer, personally. No CI/CD tools for now.
## Usage
Prerequisites: Java 8.

1. Install Maven.
1. Build the project (`package` phase is sufficient, e.g. the following command: `mvn clean package`).
1. Navigate to a `target` directory and launch the resulting JAR: `java -jar csv-etl-1.0.0.jar` - more usage instructions will follow.