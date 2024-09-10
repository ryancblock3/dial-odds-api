FROM openjdk:17-jdk-slim

# Install WireGuard
RUN apt-get update && apt-get install -y wireguard-tools

WORKDIR /app
COPY *.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]