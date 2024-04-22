FROM eclipse-temurin:17-jre-alpine
#Possibility to set JVM options (https://www.oracle.com/technetwork/java/javase/tech/vmoptions-jsp-140102.html)
VOLUME /tmp
COPY target/marmotgraph-graphdb-sync.jar marmotgraph-graphdb-sync.jar
ENV JAVA_OPTS=""
ENTRYPOINT exec java $JAVA_OPTS -jar /marmotgraph-graphdb-sync.jar