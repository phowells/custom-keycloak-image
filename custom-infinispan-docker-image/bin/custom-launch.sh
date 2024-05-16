#!/bin/sh
# ===================================================================================
# Entry point for the image which sets environment variables from file before
# launching Infinispan.
# ===================================================================================

ARG_FILE="${GKE_TOPOLOGY_ARGUMENT_FILE:-"/opt/infinispan/etc/gke-args/.args"}"
echo $ARG_FILE

if [[ ! -f $ARG_FILE ]]; then
  echo "$ARG_FILE does not exist.  Skipping GKE argument load."
else

  echo "Loading GKE arguments from $ARG_FILE"
  echo

  if [ -n "${GKE_DEBUG}" ]; then
    echo "Printing $ARG_FILE ..."
    cat $ARG_FILE
    echo
  fi

  GKE_ARGS=$(head -n 1 $ARG_FILE)

  ALL_ARGS="$@ $GKE_ARGS"

  if [ -n "${GKE_DEBUG}" ]; then
    echo "Updated Infinispan arguments"
    echo "$ALL_ARGS"
    echo
  fi
fi

echo "Launching Infinispan"
exec ./bin/launch.sh ${ALL_ARGS}
