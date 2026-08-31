FROM eclipse-temurin:17-jre
WORKDIR /app
COPY order-service/target/order-service-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","app.jar"]

