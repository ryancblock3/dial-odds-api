FROM openjdk:17-jdk-slim
WORKDIR /app
COPY *.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]