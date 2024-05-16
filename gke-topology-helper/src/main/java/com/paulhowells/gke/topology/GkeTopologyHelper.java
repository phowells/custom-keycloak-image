package com.paulhowells.gke.topology;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class GkeTopologyHelper {
    private static final Logger logger = LoggerFactory.getLogger(GkeTopologyHelper.class);

    public static final String GKE_ARGUMENT_FILENAME = "GKE_ARGUMENT_FILENAME";
    public static final String DEFAULT_GKE_ARGUMENT_FILENAME = "/opt/infinispan/etc/gke-args/.args";

    public static final String ZONE_SUBSTRING = "/zones/";

    public static void main(String[] args) throws IOException {
        logger.debug("<main");

        logger.debug("Environment Variables:");
        Map<String, String> environmentVariables = System.getenv();
        for(String key:environmentVariables.keySet()) {
            String value = environmentVariables.get(key);
            logger.debug("{}={}", key, value);
        }

        String argumentFilename = DEFAULT_GKE_ARGUMENT_FILENAME;
        if (environmentVariables.containsKey(GKE_ARGUMENT_FILENAME)) {
            argumentFilename = environmentVariables.get(GKE_ARGUMENT_FILENAME);
        }
        logger.debug("Using GKE Argument filename '{}'", argumentFilename);

        GoogleComputeMetadataApi metadataApi = new GoogleComputeMetadataApi();

        HttpStringResponse zoneAttributeResponse = metadataApi.getInstanceAttribute("zone");

        // projects/581288541933/zones/us-west1-b
        String zoneAttribute = zoneAttributeResponse.body;
        logger.debug("Zone Attribute: {}", zoneAttributeResponse.body);

        int zoneIndex = zoneAttribute.indexOf(ZONE_SUBSTRING) + ZONE_SUBSTRING.length();

        String zone = zoneAttribute.substring(zoneIndex);
        logger.debug("Zone: {}", zone);

        int clusterIndex = zone.lastIndexOf('-');

        String cluster = zone.substring(0, clusterIndex);
        logger.debug("Cluster: {}", cluster);

        try(BufferedWriter writer = new BufferedWriter(new FileWriter(argumentFilename, false))) {

            writer.write(String.format("-Dgke.cluster.name=%s -Dgke.zone.name=%s", cluster, zone));
        }

        logger.debug(">main");
    }
}
