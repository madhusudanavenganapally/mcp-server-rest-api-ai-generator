package com.rvvcode.ai.mcp.server.service;

import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rvvcode.ai.mcp.server.dto.ProjectSpec;

@Service
public class ProjectGenerator {

    public record BootstrapProjectRequest(String projectName, String basePackage, String apiRequirements,
            String specFilePath) {
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String bootstrap(BootstrapProjectRequest request) {
        try {
            ProjectSpec spec = resolveSpec(request);

            Path projectRoot = Paths.get(spec.projectName());
            if (Files.exists(projectRoot)) {
                return "Project directory already exists: " + projectRoot.toAbsolutePath();
            }
            Files.createDirectories(projectRoot);

            createBuildGradle(projectRoot, spec);
            createSettingsGradle(projectRoot, spec);
            createStructure(projectRoot, spec);
            createApplicationClass(projectRoot, spec.basePackage());
            createApplicationYml(projectRoot);
            createDocumentation(projectRoot, spec);

            return "Project " + spec.projectName() + " bootstrapped successfully at " + projectRoot.toAbsolutePath();
        } catch (IOException e) {
            return "Failed to bootstrap project: " + e.getMessage();
        }
    }

    private ProjectSpec resolveSpec(BootstrapProjectRequest request) throws IOException {
        if (request.specFilePath() != null && !request.specFilePath().isBlank()) {
            Path specPath = Paths.get(request.specFilePath());
            if (Files.exists(specPath)) {
                return objectMapper.readValue(specPath.toFile(), com.rvvcode.ai.mcp.server.dto.ProjectSpec.class);
            }
        }
        // Fallback or merge logic could go here. For now, create spec from direct args.
        // Assuming default structure if not provided via spec.
        return new com.rvvcode.ai.mcp.server.dto.ProjectSpec(
                request.projectName(),
                request.basePackage(),
                request.apiRequirements(),
                null,
                Map.of("controller", List.of(), "service", List.of(), "repository", List.of()));
    }

    private void createBuildGradle(Path root, com.rvvcode.ai.mcp.server.dto.ProjectSpec spec) throws IOException {
        String content = """
                plugins {
                    id 'java'
                    id 'org.springframework.boot' version '3.2.3'
                    id 'io.spring.dependency-management' version '1.1.4'
                }

                group = 'com.example'
                version = '0.0.1-SNAPSHOT'

                java {
                    sourceCompatibility = '17'
                }

                configurations {
                    compileOnly {
                        extendsFrom annotationProcessor
                    }
                }

                repositories {
                    mavenCentral()
                }

                ext {
                    set('mapstructVersion', '1.5.5.Final')
                }

                dependencies {
                    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
                    implementation 'org.springframework.boot:spring-boot-starter-web'
                    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'
                    implementation "org.mapstruct:mapstruct:${mapstructVersion}"

                    runtimeOnly 'com.oracle.database.jdbc:ojdbc11'

                    compileOnly 'org.projectlombok:lombok'

                    annotationProcessor 'org.projectlombok:lombok'
                    annotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"

                    testImplementation 'org.springframework.boot:spring-boot-starter-test'
                }

                tasks.named('test') {
                    useJUnitPlatform()
                }
                """;
        Files.writeString(root.resolve("build.gradle"), content);
    }

    private void createSettingsGradle(Path root, com.rvvcode.ai.mcp.server.dto.ProjectSpec spec) throws IOException {
        String content = "rootProject.name = '" + spec.projectName() + "'\n";
        Files.writeString(root.resolve("settings.gradle"), content);
    }

    private void createStructure(Path root, com.rvvcode.ai.mcp.server.dto.ProjectSpec spec) throws IOException {
        Path srcMainJava = root.resolve("src/main/java");
        Path packagePath = srcMainJava.resolve(spec.basePackage().replace('.', '/'));

        // Standard directories
        Files.createDirectories(packagePath.resolve("domain"));
        Files.createDirectories(packagePath.resolve("mapper"));
        Files.createDirectories(packagePath.resolve("dto"));
        Files.createDirectories(root.resolve("src/main/resources"));

        // Dynamic structure from spec
        if (spec.packageStructure() != null) {
            for (Map.Entry<String, List<String>> entry : spec.packageStructure().entrySet()) {
                Path dir = packagePath.resolve(entry.getKey());
                Files.createDirectories(dir);
                // Create placeholders for classes
                if (entry.getValue() != null) {
                    for (String className : entry.getValue()) {
                        createPlaceholderClass(dir, spec.basePackage() + "." + entry.getKey(), className);
                    }
                }
            }
        } else {
            Files.createDirectories(packagePath.resolve("controller"));
            Files.createDirectories(packagePath.resolve("service"));
            Files.createDirectories(packagePath.resolve("repository"));
        }
    }

    private void createPlaceholderClass(Path dir, String packageName, String className) throws IOException {
        String content = """
                package %s;

                import org.springframework.stereotype.Component;

                @Component
                public class %s {
                }
                """.formatted(packageName, className);
        Files.writeString(dir.resolve(className + ".java"), content);
    }

    private void createDocumentation(Path root, com.rvvcode.ai.mcp.server.dto.ProjectSpec spec) throws IOException {
        if (spec.apiRequirements() != null) {
            Files.writeString(root.resolve("API_REQUIREMENTS.md"), spec.apiRequirements());
        }
        if (spec.apiGoldStandards() != null) {
            Files.writeString(root.resolve("API_STANDARDS.md"), spec.apiGoldStandards());
        }
    }

    private void createApplicationClass(Path root, String basePackage) throws IOException {
        Path packagePath = root.resolve("src/main/java").resolve(basePackage.replace('.', '/'));
        String content = """
                package %s;

                import org.springframework.boot.SpringApplication;
                import org.springframework.boot.autoconfigure.SpringBootApplication;

                @SpringBootApplication
                public class Application {

                    public static void main(String[] args) {
                        SpringApplication.run(Application.class, args);
                    }
                }
                """.formatted(basePackage);
        String className = "Application.java";
        Files.writeString(packagePath.resolve(className), content);
    }

    private void createApplicationYml(Path root) throws IOException {
        String content = """
                spring:
                  datasource:
                    url: jdbc:oracle:thin:@localhost:1521/helowin
                    username: system
                    password: oracle-password
                    driver-class-name: oracle.jdbc.OracleDriver
                  jpa:
                    hibernate:
                      ddl-auto: update
                    properties:
                      hibernate:
                        dialect: org.hibernate.dialect.OracleDialect
                """;
        Files.writeString(root.resolve("src/main/resources/application.yml"), content);
    }
}
