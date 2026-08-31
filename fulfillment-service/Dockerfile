FROM eclipse-temurin:17-jre
WORKDIR /app
COPY fulfillment-service/target/fulfillment-service-*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","app.jar"]

