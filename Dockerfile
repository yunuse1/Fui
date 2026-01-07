# Builder stage: build the fat jar inside a container (no host JDK required)
FROM gradle:8.6-jdk21 AS builder
WORKDIR /home/gradle/project
COPY --chown=gradle:gradle . /home/gradle/project
# Remove android modules to avoid AGP issues in Docker
RUN rm -rf androidApp common
# Remove common from settings.gradle
RUN sed -i '/include("common")/d' settings.gradle
# Remove common dependency from build.gradle
RUN sed -i '/implementation project.*common/d' build.gradle
# Use the wrapper if present, otherwise gradle image will use installed Gradle
RUN ./gradlew shadowJar -x test --no-daemon

# Runtime stage: smaller image with just JRE
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /home/gradle/project/build/libs/fui-server.jar /app/fui-server.jar
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-Djava.awt.headless=true"
ENTRYPOINT ["java", "-jar", "/app/fui-server.jar"]
