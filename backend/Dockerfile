# syntax=docker/dockerfile:1

# =====================================================================
# Stage 1 - build
# A full Maven + JDK toolchain lives here and is thrown away afterwards.
# =====================================================================

# FROM selects the base image: Maven 3.9 running on JDK 17.
# "AS build" names this stage so stage 2 can copy files out of it.
FROM maven:3.9-eclipse-temurin-17 AS build

# WORKDIR creates /app and makes it the working directory for every
# later instruction in this stage.
WORKDIR /app

# COPY only the POM first. Every instruction creates a cached layer, so
# the slow dependency download below is repeated only when pom.xml
# changes - not on every source-code edit.
COPY pom.xml .

# RUN executes a command while building the image. This downloads the
# plugins and dependencies into the image's own Maven repository,
# which is what the cached layer above is for.
RUN mvn -B -ntp dependency:go-offline

# COPY the sources after the dependency layer, so editing Java files
# does not invalidate the download layer.
COPY src ./src

# Compile, run all JUnit tests and package the shaded, runnable jar.
# -B = batch mode, -ntp = no transfer progress (readable CI logs).
# A failing test stops the image build here, exactly like in Lab 5.
RUN mvn -B -ntp clean package

# =====================================================================
# Stage 2 - runtime
# A small JRE image: no Maven, no sources, no test reports.
# =====================================================================

# FROM starts from a clean base: JDK 17 JRE only (~270 MB) instead of
# the ~500 MB build image. eclipse-temurin is the maintained successor
# of the deprecated "openjdk:17-jre-slim" tag.
FROM eclipse-temurin:17-jre-jammy

# The JRE image ships no HTTP client, and the compose health check has to
# call /api/health from inside this container. curl also gives you a way to
# probe the API while debugging:
#   docker compose exec backend curl -s http://127.0.0.1:8000/api/health
# --no-install-recommends keeps the package list minimal, and the apt lists
# are removed in the same layer so they do not stay in the image.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# WORKDIR sets the application directory inside the final container.
WORKDIR /app

# COPY --from takes one file out of the build stage. Everything else -
# Maven, the sources, target/classes, the test reports - is discarded,
# which is the whole point of a multi-stage build.
COPY --from=build /app/target/calculator-*.jar app.jar

# EXPOSE documents the port the process listens on. It does not publish
# anything by itself; the host side is opened by `ports` in compose.
EXPOSE 8000

# ENTRYPOINT is the process started when the container runs. The exec
# form (JSON array) passes arguments straight to the JVM without a
# shell in between, and appends runtime arguments to it:
#   docker run --rm calculator-backend:lab7          -> starts the HTTP server
#   docker run --rm calculator-backend:lab7 2 + 2    -> java -jar app.jar 2 + 2
# The second form keeps the Lab 6 command-line usage working, which is why
# ENTRYPOINT is used instead of CMD: CMD would be replaced by those
# arguments instead of receiving them.
ENTRYPOINT ["java", "-jar", "app.jar"]
