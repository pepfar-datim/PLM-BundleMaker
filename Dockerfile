#Build BundleMaker
FROM maven:3.6.0-jdk-11-slim AS build
COPY . /app/
RUN mvn -f /app/ clean package
#Package
FROM openjdk:8-jdk-alpine
MAINTAINER datim.org
ENV APP_VER=0.1.0
COPY --from=build /app/target/bundleMaker-${APP_VER}.jar /app/target/bundleMaker.jar
EXPOSE 9000
CMD ["java","-jar","/app/target/bundleMaker.jar"]