FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
COPY target/marmotgraph-jsonld.jar marmotgraph-jsonld.jar
ENV JAVA_OPTS=""
ENTRYPOINT exec java $JAVA_OPTS -jar /marmotgraph-jsonld.jar