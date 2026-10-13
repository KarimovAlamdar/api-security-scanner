# ---------- Build stage: compile and package the executable JAR ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- Runtime stage: slim JRE only ----------
FROM eclipse-temurin:17-jre-alpine

LABEL org.opencontainers.image.title="API Security Scanner"
LABEL org.opencontainers.image.description="DAST + SAST Hybrid Scanner with BOLA/IDOR matrix testing, GitHub PR suggestions, and SARIF export"
LABEL org.opencontainers.image.version="1.2.0"

# Supported environment variables:
# SCANNER_AUTH_TOKEN / INPUT_AUTH-TOKEN
# SCANNER_USER_A_TOKEN / INPUT_USER-A-TOKEN
# SCANNER_USER_B_TOKEN / INPUT_USER-B-TOKEN
# SCANNER_SARIF_OUTPUT / INPUT_SARIF-OUTPUT
# SCANNER_SCAN_SOURCE / INPUT_SCAN-SOURCE
# GITHUB_TOKEN / INPUT_GITHUB-TOKEN

# GitHub mounts the workspace owned by the runner user, so the container stays root
# (a non-root user could not read the spec or write the JSON/SARIF report there).
COPY --from=build /build/target/api-security-scanner.jar /app/api-security-scanner.jar

ENTRYPOINT ["java", "-jar", "/app/api-security-scanner.jar"]
