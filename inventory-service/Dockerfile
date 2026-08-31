FROM eclipse-temurin:17-jre
WORKDIR /app
COPY inventory-service/target/inventory-service-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","app.jar"]

