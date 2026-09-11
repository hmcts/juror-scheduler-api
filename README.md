# Juror API Scheduler Service

## Prerequisites

- [Java 21](https://www.oracle.com/java)
- [Docker](https://www.docker.com)

## Environmental Variables

The following application settings are required to run the application. They can be set in the
`application-local.yaml`file or as environment variables.The application will look for the environment variables first.

```
DB_HOST=localhost
DB_PORT=5432
DB_NAME=scheduler_application
DB_USER_NAME=system
DB_PASSWORD=postgres

SECRET=
JUROR_API_SERVICE_AUTHENTICATION_SECRET=
API_JOB_EXECUTION_SERVICE_AUTHENTICATION_SECRET=

SPRING_PROFILES_ACTIVE=test
```

It is possible to configure IntelliJ to use these environment variables when running the application by setting up
default configurations. This can be done by going to `Run -> Edit Configurations` and setting the environment variables

Alternatively, its possible export the environment variables in the terminal before running the application. For example:

```bash
export DB_PASSWORD=secret
export DB_URL=jdbc:postgresql://localhost:5432/juror
export DB_USERNAME=juror
......
```

## Database setup

The application requires a Postgres database.

The latest official image of Postgres can be run from Docker Hub (https://hub.docker.com/_/postgres). To install, run the following command

```bash
docker pull postgres
```

create a local data folder, e.g. in the home directory
```bash
mkdir ~/postgres
cd ~/postgres
mkdir data
```
Then to spin up a container run the following command:
```bash
docker run --name postgres-db -p 5432:5432 -e POSTGRES_USER=system -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=scheduler_application -d postgres
```
From the root of the project, run the following command to create the database schema:
```bash
./gradlew flywayMigrate
```

## Building the application

When running the application for the first time you will need to create an ADMIN user.

To do this you will need to add the following environmental variables which (assuming the user does not already exist) create an admin user for the provided email address and password.

### Jacoco Coverage Report

A local jacoco coverage report can be generated using the following command:-

```bash
  ./gradlew jacocoTestReport
```

The report will be available under ./build/jacocoHtml/index.html. The report incorporates both unit test
and integration test coverage

## Building the application

The project uses [Gradle](https://gradle.org) as a build tool. It already contains
`./gradlew` wrapper script, so there's no need to install gradle.

To build the project execute the following command:

```bash
  ./gradlew build
```

### Running the application locally via the terminal

Create the image of the application by executing the following command:

```bash
  ./gradlew bootRun
```

This will start the API exposing the application's port
(set to `8080` in this template app).

In order to test if the application is up, you can call its health endpoint:

```bash
  curl http://localhost:8080/health
```

You should get a response similar to this:

```
  {"status":"UP","components":{"db":{"status":"UP","details":{"database":"PostgreSQL","validationQuery":"isValid()"}}
  ,"diskSpace":{"status":"UP","details":{"total":99466258420,"free":781324570624,"threshold":10485760,
  "path":"/Users/<user_name>/workspace/hmcts/juror-scheduler-api/.","exists":true}},"ping":{"status":"UP"}}}
```

It is possible to debug the application through Intellij selecting the appropriate run configuration or through gradle.

### Swagger UI

The application has Swagger enabled. To access the Swagger UI, navigate to `http://localhost:8080/swagger-ui.html`

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
