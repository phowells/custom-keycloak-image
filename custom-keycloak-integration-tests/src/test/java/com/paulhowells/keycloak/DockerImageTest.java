package com.paulhowells.keycloak;


import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

public class DockerImageTest {

    static GenericContainer container = new GenericContainer(
            new ImageFromDockerfile()
                    .withFileFromClasspath("Dockerfile", "Dockerfile"));

    @BeforeAll
    static void beforeAll() {
        container.start();
    }

    @AfterAll
    static void afterAll() {
        container.stop();
    }

    @Test
    public void test() {

    }

}
