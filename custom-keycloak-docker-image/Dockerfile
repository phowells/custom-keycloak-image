ARG KEYCLOAK_VERSION="23.0.6"

FROM quay.io/keycloak/keycloak:${KEYCLOAK_VERSION} as builder

ENV KC_DB=postgres
ENV KC_HEALTH_ENABLED=true

COPY providers/ /opt/keycloak/providers/

RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:${KEYCLOAK_VERSION}

COPY --from=builder /opt/keycloak/ /opt/keycloak/

WORKDIR /opt/keycloak
