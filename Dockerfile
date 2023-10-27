FROM folioci/alpine-jre-openjdk17:latest

ENV SYSTEM_USER_PASSWORD dcb-system-user
# Copy your fat jar to the container
ENV APP_FILE mod-dcb.jar
# - should be a single jar file
ARG JAR_FILE=./target/*.jar
# - copy
COPY ${JAR_FILE} ${JAVA_APP_DIR}/${APP_FILE}

# Expose this port locally in the container.
EXPOSE 8081
