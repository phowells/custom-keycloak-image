# The extensions image will use the Maven pom to build the extension jars
FROM maven:3.8.2-openjdk-17 as extensions-image
ENV BUILD_DIR=/usr/build
RUN mkdir -p $BUILD_DIR
COPY ./keycloak-extensions $BUILD_DIR
WORKDIR $BUILD_DIR
RUN mvn clean package

FROM us-west1-docker.pkg.dev/keycloak-infinispan-poc/infinispan-poc-repo/rhbk/keycloak-rhel9:24-7 as builder

# We will use the x-forwarded headers to support White Label domains
ENV KC_HOSTNAME_STRICT: "false"
# Enables communication through HTTP between the proxy and Red Hat build of Keycloak. This mode is suitable for
# deployments with a highly secure internal network where the reverse proxy keeps a secure connection (HTTP over TLS)
# with clients while communicating with Red Hat build of Keycloak using HTTP.
ENV KC_PROXY: "edge"
# Enable parsing of the X-Forwarded-* headers
ENV KC_PROXY_HEADERS: "xforwarded"
# Use the Postgres database for persistence
ENV KC_DB=postgres
# Enable the health check endpoint
ENV KC_HEALTH_ENABLED=true
# Enable the metrics endpoint
ENV KC_METRICS_ENABLED=true
# Configure Keycloak to use Infinispan caching
ENV KC_CACHE: ispn
ENV KC_CACHE_CONFIG_FILE=external-cache-ispn.xml
# Configure Keycloak to use DNS PING for discovery
ENV KC_CACHE_STACK=kubernetes

COPY providers/ /opt/keycloak/providers/

COPY conf/ /opt/keycloak/conf/

RUN /opt/keycloak/bin/kc.sh build

FROM us-west1-docker.pkg.dev/keycloak-infinispan-poc/infinispan-poc-repo/rhbk/keycloak-rhel9:24-7

COPY --from=builder /opt/keycloak/ /opt/keycloak/

WORKDIR /opt/keycloak
