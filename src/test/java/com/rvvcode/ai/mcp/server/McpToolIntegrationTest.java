package com.rvvcode.ai.mcp.server;

import com.rvvcode.ai.mcp.server.service.DomainEnhancer;
import com.rvvcode.ai.mcp.server.service.ProjectGenerator;
import com.rvvcode.ai.mcp.server.service.TestGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class McpToolIntegrationTest {

    @Autowired
    private ProjectGenerator projectGenerator;

    @Autowired
    private DomainEnhancer domainEnhancer;

    @Autowired
    private TestGenerator testGenerator;

    @Test
    void tools_shouldWorkTogetherOnGeneratedProject(@TempDir Path tempDir) throws Exception {
        Path originalUserDir = Path.of(System.getProperty("user.dir"));
        Path projectRoot = tempDir.resolve("platform-app");

        String bootstrapResult = projectGenerator.bootstrapProject(
                projectRoot.toString(),
                "com.acme.platform",
                "REST APIs for enterprise customer workflows");

        assertThat(bootstrapResult).contains("Bootstrapped enterprise project");
        assertThat(Files.exists(projectRoot.resolve("pom.xml"))).isTrue();

        System.setProperty("user.dir", projectRoot.toString());
        try {
            String enhanceResult = domainEnhancer.enhanceDomain(
                    "Account",
                    Map.of("email", "String", "active", "Boolean"),
                    List.of("java.util.Optional<AccountEntity> findByEmail(String email)"));
            assertThat(enhanceResult).contains("Enhanced domain for Account");

            Path accountService = projectRoot.resolve("src/main/java/com/acme/platform/account/AccountService.java");
            assertThat(Files.exists(accountService)).isTrue();

            String serviceSource = Files.readString(accountService);
            String testsResult = testGenerator.generateTests("AccountService", serviceSource);
            assertThat(testsResult).contains("AccountServiceTest");
            assertThat(Files.exists(projectRoot.resolve("generated-tests/AccountServiceTest.java"))).isTrue();
        } finally {
            System.setProperty("user.dir", originalUserDir.toString());
        }
    }
}
