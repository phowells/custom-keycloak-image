package com.paulhowells.keycloak;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

public class KeycloakContainer extends GenericContainer<KeycloakContainer> {

    public KeycloakContainer() {
        super(new ImageFromDockerfile()
                .withFileFromClasspath("Dockerfile", "Dockerfile"));
    }
}
