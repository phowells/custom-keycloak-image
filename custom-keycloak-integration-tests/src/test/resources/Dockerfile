FROM us-west1-docker.pkg.dev/keycloak-infinispan-poc/infinispan-poc-repo/rhbk/keycloak-rhel9:24-7 as builder

ENV KC_LOG_LEVEL=INFO

RUN /opt/keycloak/bin/kc.sh build

FROM us-west1-docker.pkg.dev/keycloak-infinispan-poc/infinispan-poc-repo/rhbk/keycloak-rhel9:24-7

COPY --from=builder /opt/keycloak/ /opt/keycloak/

WORKDIR /opt/keycloak
