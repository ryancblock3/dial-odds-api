FROM openjdk:17-jdk-slim

# Install WireGuard
RUN apt-get update && apt-get install -y wireguard-tools

WORKDIR /app

# Copy the JAR file from the build context to the container
COPY target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]