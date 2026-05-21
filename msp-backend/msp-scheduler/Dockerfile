FROM eclipse-temurin:17-jdk-alpine AS builder

RUN apk add --no-cache maven

WORKDIR /app

COPY pom.xml .
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

RUN mvn install -DskipTests -pl msp-common,msp-kuscia -am && \
    mvn package -DskipTests -pl msp-scheduler -am

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /app/msp-scheduler/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]