package com.rvvcode.ai.mcp.server.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectGeneratorTest {

    private final ProjectGenerator projectGenerator = new ProjectGenerator();

    @Test
    void bootstrapProject_shouldCreateEnterpriseSkeleton(@TempDir Path tempDir) throws Exception {
        Path projectRoot = tempDir.resolve("enterprise-app");

        String result = projectGenerator.bootstrapProject(
                projectRoot.toString(),
                "com.acme.enterprise",
                "Expose customer APIs");

        assertThat(result).contains("Bootstrapped enterprise project");
        assertThat(Files.exists(projectRoot.resolve("pom.xml"))).isTrue();
        assertThat(Files.exists(projectRoot.resolve("README.md"))).isTrue();
        assertThat(Files.exists(projectRoot.resolve("src/main/resources/application.yml"))).isTrue();
        assertThat(Files.exists(projectRoot.resolve("src/main/java/com/acme/enterprise/customer/CustomerController.java"))).isTrue();
        assertThat(Files.exists(projectRoot.resolve("src/main/java/com/acme/enterprise/customer/CustomerService.java"))).isTrue();
        assertThat(Files.exists(projectRoot.resolve("src/main/java/com/acme/enterprise/customer/CustomerRepository.java"))).isTrue();

        String pom = Files.readString(projectRoot.resolve("pom.xml"));
        assertThat(pom).contains("ojdbc11").contains("mapstruct").contains("springdoc-openapi-starter-webmvc-ui");
    }

    @Test
    void bootstrapProject_shouldReturnAlreadyExistsWhenFolderExists(@TempDir Path tempDir) throws Exception {
        Path existing = tempDir.resolve("existing-project");
        Files.createDirectories(existing);

        String result = projectGenerator.bootstrapProject(existing.toString(), "com.acme.app", "req");

        assertThat(result).contains("Project already exists");
    }
}
