#!/bin/sh
# ===================================================================================
# Entry point for the GKE Topology Helper
# ===================================================================================

ARGS="$@"
echo "Launching GKE Topology Helper"
exec java -cp ./libs/gke-topology-helper-${project.parent.version}-jar-with-dependencies.jar com.paulhowells.gke.topology.GkeTopologyHelper ${ARGS}
