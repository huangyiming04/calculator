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
java -jar target/calculator-1.0.jar          # HTTP service on port 8000
java -jar target/calculator-1.0.jar 2 + 2    # command-line mode
```

Supported operators are `+`, `-`, `*` and `/`.

### API

| Request | Result |
| --- | --- |
| `GET /api/health` | `{"status":"ok"}` |
| `GET /api/calc?a=2&op=%2B&b=3` | `{"a":"2","op":"+","b":"3","result":5.0}` |

Missing or invalid input answers `400`, division by zero answers `422`.

## Run in Docker

The backend image is built in two stages: Maven compiles and tests the project,
then only the runnable jar is copied into a small JRE image.

```bash
docker build -t calculator-backend:lab7 backend
docker run --rm calculator-backend:lab7 2 + 2
```

## Run with Docker Compose

`docker-compose.yml` starts both services on one private network. nginx serves
the page on port 8080 and forwards `/api/` to the backend service by name, so
the page never needs to know where the API lives.

```bash
docker compose up -d --build
# open http://127.0.0.1:8080
docker compose ps
docker compose logs -f
docker compose down
```

## Tooling

| Tool | Purpose | Command |
| --- | --- | --- |
| JUnit 5 | unit tests | `mvn test` |
| JaCoCo | code coverage, reports in `target/site/jacoco/` | `mvn test` |
| SpotBugs | static analysis | `mvn spotbugs:check` |
