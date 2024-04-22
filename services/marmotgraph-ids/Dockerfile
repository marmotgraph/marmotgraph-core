FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
COPY target/marmotgraph-ids.jar marmotgraph-ids.jar
ENV JAVA_OPTS=""
ENTRYPOINT exec java $JAVA_OPTS -jar /marmotgraph-ids.jar