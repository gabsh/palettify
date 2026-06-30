FROM mcr.microsoft.com/playwright/java:v1.44.0-jammy

WORKDIR /app

COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw package -DskipTests -B

EXPOSE 8080

CMD ["java", "-jar", "target/palettify-0.0.1-SNAPSHOT.jar"]