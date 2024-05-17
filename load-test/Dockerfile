# Use official Maven image as a build environment
FROM maven:3.8.3-openjdk-17 AS build

# Set the working directory in the container
WORKDIR /app

# Copy the Maven project descriptor
COPY pom.xml .

RUN mvn install -DskipTests

# Copy the rest of the project
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Use AdoptOpenJDK as a runtime environment
FROM eclipse-temurin:17-jre-alpine

# Set the working directory in the container
WORKDIR /app

COPY start-jar.sh start-jar.sh
RUN chmod +x start-jar.sh

# Copy the packaged JAR file from the build stage to the container
COPY --from=build /app/target/*-jar-with-dependencies.jar app.jar

# Define command to run the application when the container starts
ENTRYPOINT /app/start-jar.sh