FROM us-west1-docker.pkg.dev/keycloak-infinispan-poc/infinispan-poc-repo/ubi8/openjdk-17-runtime:1.18-1 as builder

COPY libs/ /opt/gke-topology-helper/libs/

COPY bin/ /opt/gke-topology-helper/bin/

# Define the working directory
WORKDIR /opt/gke-topology-helper
# Define entrypoint
ENTRYPOINT ["./bin/run.sh"]
