FROM us-west1-docker.pkg.dev/keycloak-infinispan-poc/infinispan-poc-repo/rhbk/keycloak-rhel9:24-7 as dependency-image

FROM us-west1-docker.pkg.dev/keycloak-infinispan-poc/infinispan-poc-repo/datagrid/datagrid-8-rhel8:1.4-8.4.7.GA as builder

# Copy the dependency jars to the Infinispan image
# Example JAR file names
# org.keycloak.keycloak-model-infinispan-24.0.3.redhat-00004.jar
# org.keycloak.keycloak-server-spi-private-24.0.3.redhat-00004.jar
# org.keycloak.keycloak-server-spi-24.0.3.redhat-00004.jar
# org.keycloak.keycloak-core-24.0.3.redhat-00004.jar

COPY --from=dependency-image /opt/keycloak/lib/lib/main/org.keycloak.keycloak-model-infinispan-[0-9]*.jar /opt/infinispan/server/lib/
COPY --from=dependency-image /opt/keycloak/lib/lib/main/org.keycloak.keycloak-server-spi-private-[0-9]*.jar /opt/infinispan/server/lib/
COPY --from=dependency-image /opt/keycloak/lib/lib/main/org.keycloak.keycloak-server-spi-[0-9]*.jar /opt/infinispan/server/lib/
COPY --from=dependency-image /opt/keycloak/lib/lib/main/org.keycloak.keycloak-core-[0-9]*.jar /opt/infinispan/server/lib/

COPY bin/ /opt/infinispan/bin/

# Define the working directory
WORKDIR /opt/infinispan
# Define entrypoint
ENTRYPOINT ["./bin/custom-launch.sh"]
