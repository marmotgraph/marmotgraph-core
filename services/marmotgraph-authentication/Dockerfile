FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
COPY target/marmotgraph-authentication.jar marmotgraph-authentication.jar
ENV JAVA_OPTS=""
ENTRYPOINT exec java $JAVA_OPTS -jar /marmotgraph-authentication.jar