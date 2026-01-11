FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY build/libs/fui-server.jar /app/fui-server.jar
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-Djava.awt.headless=true"
ENTRYPOINT ["java", "-jar", "/app/fui-server.jar"]

