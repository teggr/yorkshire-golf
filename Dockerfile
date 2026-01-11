FROM eclipse-temurin:21-jre
LABEL authors="robin"
LABEL service="teggr/yorkshire-golf:${project.version}"
COPY target/golf-tracker-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
