FROM eclipse-temurin:24-jre-alpine
VOLUME /tmp
COPY target/marmotgraph-core-allinone.jar marmotgraph-core-allinone.jar
ENV JAVA_OPTS=""
ENTRYPOINT exec java $JAVA_OPTS -jar /marmotgraph-core-allinone.jar