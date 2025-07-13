# Stage 1: Dependencies
FROM gradle:8.4.0-jdk17 AS dependencies
WORKDIR /build

COPY gradlew .
# COPY gradle.properties /root/.gradle/gradle.properties
COPY gradle/wrapper/gradle-wrapper.jar gradle/wrapper/
COPY gradle/wrapper/gradle-wrapper.properties gradle/wrapper/
COPY build.gradle settings.gradle ./

RUN ./gradlew dependencies --no-daemon

# Stage 2: Build
FROM gradle:8.4.0-jdk17 AS builder
WORKDIR /build

COPY --from=dependencies /build /build
COPY src src

RUN ./gradlew bootJar -x test --build-cache --no-daemon

# Stage 3: Final Image
FROM openjdk:17-jdk-slim

ENV TZ=Asia/Seoul
RUN ln -sf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ >/etc/timezone

WORKDIR /app

COPY --from=builder /build/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]