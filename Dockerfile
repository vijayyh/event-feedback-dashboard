# Event Feedback Dashboard - container image
#
# Packages the WAR that Maven built and tested (`mvn verify`), so the image contains exactly the tested artefact.
# The WAR is executable: `java -jar` starts it with its embedded Tomcat.
#
#   mvn -B clean verify
#   docker build -t event-feedback-dashboard:1.0.0 --build-arg VERSION=1.0.0 --build-arg REVISION=$(git rev-parse --short HEAD) .
#   docker run -d --name efd -p 8088:8080 -e SPRING_SECURITY_USER_PASSWORD=<organiser password> -v efd-data:/data event-feedback-dashboard:1.0.0
FROM eclipse-temurin:21-jre-alpine

ARG VERSION=dev
ARG REVISION=unknown
LABEL org.opencontainers.image.title="Event Feedback Dashboard" \
      org.opencontainers.image.description="Collects event feedback, shows ratings, indicators and alerts" \
      org.opencontainers.image.source="https://github.com/vijayyh/event-feedback-dashboard" \
      org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.revision="${REVISION}"

# Run as an unprivileged user. The H2 database lives in /data; mount a volume there to keep it across containers.
RUN addgroup -S efd && adduser -S -G efd -h /app efd \
    && mkdir -p /data && chown efd:efd /data
WORKDIR /app
COPY --chown=efd:efd target/event-feedback-dashboard.war app.war
USER efd

# Settings are environment variables (Spring Boot maps APP_ENVIRONMENT to app.environment, and so on).
# The organiser password is passed at run time: -e SPRING_SECURITY_USER_PASSWORD=...
ENV SERVER_PORT=8080 \
    APP_ENVIRONMENT=docker \
    APP_DATA_DIR=/data \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
VOLUME ["/data"]

# Docker marks the container healthy once Spring Boot reports UP
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.war"]
