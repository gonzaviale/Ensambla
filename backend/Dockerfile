# ---- Etapa de build ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Primero el wrapper y el pom, para cachear las dependencias en una capa propia
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src src
RUN ./mvnw -B -DskipTests package && cp target/*.jar app.jar

# ---- Etapa de ejecución ----
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system ensambla && useradd --system --gid ensambla ensambla
COPY --from=build /app/app.jar app.jar
USER ensambla

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"

# Render inyecta PORT; el perfil prod usa server.port=${PORT}
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
