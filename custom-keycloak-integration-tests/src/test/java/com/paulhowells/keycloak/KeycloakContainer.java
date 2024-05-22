package com.paulhowells.keycloak;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.time.Duration;

public class KeycloakContainer extends GenericContainer<KeycloakContainer> {

    private static final int HTTP_PORT_INTERNAL = 8080;

    private static final long WAIT_TIMEOUT_IN_SECONDS = 60L;

    public KeycloakContainer() {
        super(new ImageFromDockerfile()
                .withFileFromClasspath("Dockerfile", "Dockerfile"));
    }

    public String getUrl() {

        return String.format("http://%s:%s", getHost(), getMappedPort(HTTP_PORT_INTERNAL));
    }

    @Override
    protected void configure() {
        super.configure();

        this.addEnv("KC_HTTP_PORT", String.valueOf(HTTP_PORT_INTERNAL));
        this.withExposedPorts(HTTP_PORT_INTERNAL);

        combinedWaitAllStrategy(waitForListeningPort());
    }

    private WaitStrategy waitForListeningPort() {
        return Wait
                .forListeningPort()
                .withStartupTimeout(Duration.ofSeconds(WAIT_TIMEOUT_IN_SECONDS));
    }

    private void combinedWaitAllStrategy(WaitStrategy waitStrategy) {
        WaitAllStrategy waitAll = new WaitAllStrategy()
                .withStartupTimeout(Duration.ofSeconds(WAIT_TIMEOUT_IN_SECONDS));
        WaitStrategy currentWaitStrategy = getWaitStrategy();
        if (currentWaitStrategy != null) {
            waitAll.withStrategy(currentWaitStrategy);
        }
        waitAll.withStrategy(waitStrategy);
    }
}
