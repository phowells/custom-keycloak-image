FROM quay.io/keycloak/keycloak:24.0.4 as builder

ENV KC_LOG_LEVEL=WARN

COPY providers/ /opt/keycloak/providers/

RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:24.0.4

COPY --from=builder /opt/keycloak/ /opt/keycloak/

WORKDIR /opt/keycloak
