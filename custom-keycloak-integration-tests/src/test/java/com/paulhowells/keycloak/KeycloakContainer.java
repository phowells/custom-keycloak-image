package com.paulhowells.keycloak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.stream.Stream;

public class KeycloakContainer extends GenericContainer<KeycloakContainer> {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakContainer.class);

    private static final int HTTP_PORT_INTERNAL = 8080;

    private static final long WAIT_TIMEOUT_IN_SECONDS = 60L;

    public KeycloakContainer() {
        super(getImageFromDockerfile());
    }

    private static ImageFromDockerfile getImageFromDockerfile() {
        logger.debug("<getImageFromDockerfile");
        ImageFromDockerfile result = new ImageFromDockerfile();

        result.withFileFromClasspath("Dockerfile", "Dockerfile");

        Path providersPath = Paths.get("providers");

//        try (Stream<Path> paths = Files.walk(Paths.get("/home/paul_steven_howells/dev/custom-keycloak-image/custom-keycloak-integration-tests/providers"))) {
        try (Stream<Path> paths = Files.walk(providersPath)) {
            paths
                    .filter(Files::isRegularFile)
                    .forEach(p -> {
                        String filename = String.format("providers/%s",p.getFileName());
                        logger.debug(filename+"="+p);
                        result.withFileFromPath(filename, p);
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.debug(">getImageFromDockerfile");
        return result;
    }

    public String getUrl() {

        return String.format("http://%s:%s", getHost(), getMappedPort(HTTP_PORT_INTERNAL));
    }

    @Override
    protected void configure() {
        super.configure();

        this.withCommand("start-dev");
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
