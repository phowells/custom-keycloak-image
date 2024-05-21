FROM us-west1-docker.pkg.dev/keycloak-infinispan-poc/infinispan-poc-repo/ubi8/openjdk-17-runtime:1.18-1 as builder

COPY libs/ /opt/keycloak-configurer/libs/

COPY bin/ /opt/keycloak-configurer/bin/

# Define the working directory
WORKDIR /opt/keycloak-configurer
# Define entrypoint
ENTRYPOINT ["./bin/run.sh"]