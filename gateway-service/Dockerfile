FROM eclipse-temurin:17-jre
WORKDIR /app
COPY gateway-service/target/gateway-service-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","app.jar"]

