#!/bin/sh
# ===================================================================================
# Entry point for the Keycloak Configurer
# ===================================================================================

ARGS="$@"
echo "Launching Keycloak Configurer"
exec java -cp ./libs/custom-keycloak-configurer-${project.parent.version}-jar-with-dependencies.jar com.paulhowells.keycloak.configurer.KeycloakConfigurer ${ARGS}
