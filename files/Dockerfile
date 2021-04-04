# IMPORTANT:
# Since this file will be processed by sed cmd to replace the placeholders,
# please add backslash before double quote (i.e. \").

FROM openjdk:${CONTAINER_JAVA_VERSION}

# TZ is used by oracle jdbc
ENV TZ=Asia/Taipei

RUN mkdir -p /usr/src/${APP_NAME}

COPY ./target/*.jar /usr/src/${APP_NAME}/

CMD java -jar ${APP_JAVA_OPTS} /usr/src/${APP_NAME}/*.jar --server.servlet.context-path=/${APP_NAME}