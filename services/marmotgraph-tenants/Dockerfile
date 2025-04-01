FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
COPY target/marmotgraph-config.jar marmotgraph-config.jar
ENV JAVA_OPTS=""
ENTRYPOINT exec java $JAVA_OPTS -jar /marmotgraph-config.jar