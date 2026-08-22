# ============================================================
# Etapa 1: compilacion
# Se usa una imagen con Maven para construir el .jar.
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Primero solo el pom.xml: si no cambia, Docker reutiliza la capa con las
# dependencias ya descargadas y el build siguiente es mucho mas rapido.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Ahora si el codigo fuente.
COPY src ./src
RUN mvn clean package -DskipTests -B

# ============================================================
# Etapa 2: ejecucion
# Imagen final chica: solo el JRE y el .jar, sin Maven ni el codigo fuente.
# ============================================================
FROM eclipse-temurin:17-jre-alpine

# curl lo necesita el healthcheck de docker-compose.
RUN apk add --no-cache curl

# Usuario sin privilegios: si alguien vulnera la app, no queda como root.
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN chown -R spring:spring /app
USER spring:spring

EXPOSE 8080

# Le avisa a la JVM que respete los limites de memoria del contenedor.
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
