FROM eclipse-temurin:17-jre-jammy
ARG JAR_FILE=target/stock-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app_stock.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app_stock.jar"]
