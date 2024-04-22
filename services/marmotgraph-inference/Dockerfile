FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
COPY target/marmotgraph-inference.jar marmotgraph-inference.jar
ENV JAVA_OPTS=""
ENTRYPOINT exec java $JAVA_OPTS -jar /marmotgraph-inference.jar