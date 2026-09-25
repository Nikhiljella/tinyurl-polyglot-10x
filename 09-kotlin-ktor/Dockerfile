FROM gradle:8.7-jdk17 AS build
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle . .
RUN gradle buildFatJar --no-daemon || gradle installDist --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /home/gradle/src/build/install/tinyurl-kotlin-ktor /app
ENV PORT=8009
EXPOSE 8009
CMD ["/app/bin/tinyurl-kotlin-ktor"]
