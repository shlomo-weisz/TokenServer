FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY certs/netfree-ca-bundle-curl.crt /usr/local/share/ca-certificates/netfree-ca-bundle.crt
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && update-ca-certificates \
    && csplit -s -z -f /tmp/netfree-cert- /usr/local/share/ca-certificates/netfree-ca-bundle.crt '/-----BEGIN CERTIFICATE-----/' '{*}' \
    && for cert in /tmp/netfree-cert-*; do if grep -q "BEGIN CERTIFICATE" "$cert"; then keytool -importcert -noprompt -trustcacerts -alias "$(basename "$cert")" -file "$cert" -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit; fi; done \
    && rm -f /tmp/netfree-cert-*
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY certs/netfree-ca-bundle-curl.crt /usr/local/share/ca-certificates/netfree-ca-bundle.crt
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && update-ca-certificates \
    && csplit -s -z -f /tmp/netfree-cert- /usr/local/share/ca-certificates/netfree-ca-bundle.crt '/-----BEGIN CERTIFICATE-----/' '{*}' \
    && for cert in /tmp/netfree-cert-*; do if grep -q "BEGIN CERTIFICATE" "$cert"; then keytool -importcert -noprompt -trustcacerts -alias "$(basename "$cert")" -file "$cert" -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit; fi; done \
    && rm -f /tmp/netfree-cert-*
COPY --from=build /app/target/tokenlearn-server-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
