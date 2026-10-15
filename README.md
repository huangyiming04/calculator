# Calculator

[![Java CI](https://github.com/huangyiming04/calculator/actions/workflows/maven.yml/badge.svg?branch=main)](https://github.com/huangyiming04/calculator/actions/workflows/maven.yml?query=branch%3Amain)

A small command-line calculator built with Java 17 and Maven, used to practise
unit testing, debugging and static analysis.

## Build and test

```bash
mvn clean test
```

## Run

```bash
mvn package
java -jar target/calculator-1.0.jar 2 + 2
```

Supported operators are `+`, `-`, `*` and `/`.

## Tooling

| Tool | Purpose | Command |
| --- | --- | --- |
| JUnit 5 | unit tests | `mvn test` |
| JaCoCo | code coverage, reports in `target/site/jacoco/` | `mvn test` |
| SpotBugs | static analysis | `mvn spotbugs:check` |
