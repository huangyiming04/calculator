# Calculator

[![Java CI](https://github.com/huangyiming04/calculator/actions/workflows/maven.yml/badge.svg?branch=main)](https://github.com/huangyiming04/calculator/actions/workflows/maven.yml?query=branch%3Amain)

A small calculator built with Java 17 and Maven, used to practise unit testing,
debugging, static analysis, containerisation and multi-container deployment.

## Project layout

| Path | Contents |
| --- | --- |
| `backend/` | Calculator logic, unit tests, Maven build and the backend Dockerfile |
| `web/` | Frontend page served by nginx, with the backend reverse proxy |
| `docker-compose.yml` | Runs both services on one private network |
| `.github/workflows/` | CI pipeline |

## Build and test

```bash
cd backend
mvn clean test
```

## Run

```bash
cd backend
mvn package
java -jar target/calculator-1.0.jar 2 + 2
```

Supported operators are `+`, `-`, `*` and `/`.

## Run in Docker

The image is built in two stages: Maven compiles and tests the project, then
only the runnable jar is copied into a small JRE image.

```bash
docker build -t calculator:v1 backend
docker run --rm calculator:v1 2 + 2
```

## Tooling

| Tool | Purpose | Command |
| --- | --- | --- |
| JUnit 5 | unit tests | `mvn test` |
| JaCoCo | code coverage, reports in `target/site/jacoco/` | `mvn test` |
| SpotBugs | static analysis | `mvn spotbugs:check` |
