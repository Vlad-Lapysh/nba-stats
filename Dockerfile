FROM ghcr.io/graalvm/jdk-community:22

VOLUME /tmp

EXPOSE 8080
# Expose the debugging port
EXPOSE 5005

COPY build/libs/nba-*-all.jar app.jar

ENV reactor.schedulers.defaultBoundedElasticOnVirtualThreads=true

ENV JDK_JAVA_OPTIONS="\
    --add-modules java.se \
    --add-exports java.base/jdk.internal.ref=ALL-UNNAMED \
    --add-opens java.base/java.lang=ALL-UNNAMED \
    --add-opens java.base/sun.nio.ch=ALL-UNNAMED \
    --add-opens java.management/sun.management=ALL-UNNAMED \
    --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED \
    -XX:+UseZGC \
    -XX:+ZGenerational \
    -Xmx650m \
    -Xms150m \
    -Dreactor.schedulers.defaultBoundedElasticOnVirtualThreads=true \
    -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
