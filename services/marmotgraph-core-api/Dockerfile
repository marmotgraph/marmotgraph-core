FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
COPY target/marmotgraph-core-api.jar marmotgraph-core-api.jar
ENV JAVA_OPTS=""
ENTRYPOINT exec java $JAVA_OPTS -jar /marmotgraph-core-api.jar