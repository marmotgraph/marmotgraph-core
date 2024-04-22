FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
COPY target/marmotgraph-releasing.jar marmotgraph-releasing.jar
ENV JAVA_OPTS=""
ENTRYPOINT exec java $JAVA_OPTS -jar /marmotgraph-releasing.jar