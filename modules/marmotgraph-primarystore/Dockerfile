FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
COPY target/marmotgraph-primarystore.jar marmotgraph-primarystore.jar
ENV JAVA_OPTS=""
ENTRYPOINT exec java $JAVA_OPTS -jar /marmotgraph-primarystore.jar