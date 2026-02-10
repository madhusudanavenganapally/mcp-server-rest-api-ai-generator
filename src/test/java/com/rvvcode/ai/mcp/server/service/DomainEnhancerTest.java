package com.rvvcode.ai.mcp.server.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DomainEnhancerTest {

    private final DomainEnhancer domainEnhancer = new DomainEnhancer();

    @Test
    void enhanceDomain_shouldGenerateFeatureFilesWithRepositoryMethods(@TempDir Path tempDir) throws Exception {
        Path originalUserDir = Path.of(System.getProperty("user.dir"));
        Path projectRoot = tempDir.resolve("domain-project");
        Files.createDirectories(projectRoot.resolve("src/main/java/com/example/demo"));
        Files.writeString(projectRoot.resolve("src/main/java/com/example/demo/Application.java"),
                "package com.example.demo; public class Application {}\n");

        System.setProperty("user.dir", projectRoot.toString());
        try {
            String result = domainEnhancer.enhanceDomain(
                    "Customer",
                    Map.of("email", "String", "status", "String"),
                    List.of("java.util.Optional<CustomerEntity> findByEmail(String email)", "java.util.List<CustomerEntity> findByStatus(String status)"));

            Path featureDir = projectRoot.resolve("src/main/java/com/example/demo/customer");
            assertThat(result).contains("Enhanced domain for Customer");
            assertThat(Files.exists(featureDir.resolve("CustomerEntity.java"))).isTrue();
            assertThat(Files.exists(featureDir.resolve("CustomerDto.java"))).isTrue();
            assertThat(Files.exists(featureDir.resolve("CustomerMapper.java"))).isTrue();
            assertThat(Files.exists(featureDir.resolve("CustomerRepository.java"))).isTrue();
            assertThat(Files.exists(featureDir.resolve("CustomerService.java"))).isTrue();
            assertThat(Files.exists(featureDir.resolve("CustomerController.java"))).isTrue();

            String repository = Files.readString(featureDir.resolve("CustomerRepository.java"));
            assertThat(repository).contains("findByEmail").contains("findByStatus");

            String service = Files.readString(featureDir.resolve("CustomerService.java"));
            assertThat(service).contains("Entering CustomerService.findAll").contains("Exiting CustomerService.findAll");
        } finally {
            System.setProperty("user.dir", originalUserDir.toString());
        }
    }
}
