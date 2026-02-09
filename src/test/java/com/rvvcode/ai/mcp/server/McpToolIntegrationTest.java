package com.rvvcode.ai.mcp.server;

import com.rvvcode.ai.mcp.server.service.ProjectGenerator;
import com.rvvcode.ai.mcp.server.service.DomainEnhancer;
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

    @Test
    void testBootstrapProject(@TempDir Path tempDir) {
        Path projectLocation = tempDir.resolve("test-project");
        String projectName = projectLocation.toString();
        String basePackage = "com.test.app";

        ProjectGenerator.BootstrapProjectRequest request = new ProjectGenerator.BootstrapProjectRequest(
                projectName, basePackage, "reqs", null);

        String result = projectGenerator.bootstrap(request);

        assertThat(result).contains("bootstrapped successfully");
        assertThat(Files.exists(projectLocation.resolve("build.gradle"))).isTrue();

        // Now test enhancement on the created project
        DomainEnhancer.EnhanceDomainRequest enhanceRequest = new DomainEnhancer.EnhanceDomainRequest(
                "User",
                Map.of("username", "String", "email", "String"),
                List.of("findByEmail(String email)"),
                projectLocation.toString());

        String enhanceResult = domainEnhancer.enhance(enhanceRequest);
        assertThat(enhanceResult).contains("Domain enhanced for User");

        Path basePkgPath = projectLocation.resolve("src/main/java/com/test/app");
        // Check for Package-by-Feature structure: com.test.app.user.*
        Path featurePkgPath = basePkgPath.resolve("user");
        assertThat(Files.exists(featurePkgPath.resolve("User.java"))).isTrue();
        assertThat(Files.exists(featurePkgPath.resolve("UserRepository.java"))).isTrue();
        assertThat(Files.exists(featurePkgPath.resolve("UserService.java"))).isTrue();
        assertThat(Files.exists(featurePkgPath.resolve("UserController.java"))).isTrue();
        assertThat(Files.exists(featurePkgPath.resolve("UserDTO.java"))).isTrue();
        assertThat(Files.exists(featurePkgPath.resolve("UserMapper.java"))).isTrue();
    }

    // Note: enhanceDomain test is tricky because it relies on system property
    // "user.dir" or needs mocking of findProjectRoot.
    // Ideally DomainEnhancer should accept projectRoot as an argument for
    // testability.
    // I will refactor DomainEnhancer to accept an optional root path or use a
    // protected method we can override or spy.
    // For now, I'll stick to testing bootstrap which is the main file generator.
}
