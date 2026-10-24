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

# WORKDIR sets the application directory inside the final container.
WORKDIR /app

# COPY --from takes one file out of the build stage. Everything else -
# Maven, the sources, target/classes, the test reports - is discarded,
# which is the whole point of a multi-stage build.
COPY --from=build /app/target/calculator-*.jar app.jar

# ENTRYPOINT is the process started when the container runs. The exec
# form (JSON array) passes arguments straight to the JVM without a
# shell in between, and appends runtime arguments to it:
#   docker run --rm calculator:v1 2 + 2   ->   java -jar app.jar 2 + 2
# ENTRYPOINT is used instead of CMD because CMD would be replaced by
# those arguments instead of receiving them.
ENTRYPOINT ["java", "-jar", "app.jar"]
