FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
COPY target/marmotgraph-indexing.jar marmotgraph-indexing.jar
ENV JAVA_OPTS=""
ENTRYPOINT exec java $JAVA_OPTS -jar /marmotgraph-indexing.jar