FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .
COPY maven-settings.xml /root/.m2/settings.xml
COPY msp-common/pom.xml msp-common/
COPY msp-kuscia/pom.xml msp-kuscia/
COPY msp-scheduler/pom.xml msp-scheduler/
COPY msp-node-manager/pom.xml msp-node-manager/
COPY msp-gateway/pom.xml msp-gateway/
COPY msp-starter/pom.xml msp-starter/

COPY msp-common/src msp-common/src
COPY msp-kuscia/src msp-kuscia/src
COPY msp-scheduler/src msp-scheduler/src
COPY msp-node-manager/src msp-node-manager/src
COPY msp-gateway/src msp-gateway/src
COPY msp-starter/src msp-starter/src

RUN mvn install -DskipTests -pl msp-common -am && \
    mvn package -DskipTests -pl msp-gateway -am

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/msp-gateway/target/*.jar app.jar

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "app.jar"]