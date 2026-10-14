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
# Carpeta de fotos de cuadernos, escribible por el usuario de la app. Para que
# sobreviva a los deploys, montar un disco persistente en /app/datos (ej. Render Disk).
RUN mkdir -p /app/datos && chown -R ensambla:ensambla /app/datos
COPY --from=build /app/app.jar app.jar
USER ensambla

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75" \
    ENSAMBLA_IA_PROCESAMIENTO_ARCHIVOS_RAIZ=/app/datos/ia/cuadernos/fotos

# Render inyecta PORT; el perfil prod usa server.port=${PORT}
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
