package com.rvvcode.ai.mcp.server.service;

import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DomainEnhancer {

    public record EnhanceDomainRequest(String entityName, Map<String, String> fields, List<String> repositoryMethods,
            String projectRootPath) {
    }

    public String enhance(EnhanceDomainRequest request) {
        try {
            Path projectRoot = request.projectRootPath() != null ? Paths.get(request.projectRootPath())
                    : findProjectRoot();
            String basePackage = findBasePackage(projectRoot);

            // Package-by-Feature: create a sub-package named after the entity (lowercase)
            String featurePackageName = request.entityName().toLowerCase();
            Path featureDir = projectRoot.resolve("src/main/java")
                    .resolve(basePackage.replace('.', '/'))
                    .resolve(featurePackageName);

            String fullFeaturePackage = basePackage + "." + featurePackageName;

            Files.createDirectories(featureDir);

            createEntity(featureDir, fullFeaturePackage, request);
            createRepository(featureDir, fullFeaturePackage, request);
            createDto(featureDir, fullFeaturePackage, request);
            createMapper(featureDir, fullFeaturePackage, request);
            createService(featureDir, fullFeaturePackage, request);
            createController(featureDir, fullFeaturePackage, request);

            return "Domain enhanced for " + request.entityName() + " in package " + fullFeaturePackage;
        } catch (Exception e) {
            return "Failed to enhance domain: " + e.getMessage();
        }
    }

    private Path findProjectRoot() {
        return Paths.get(System.getProperty("user.dir"));
        // In a real scenario, we might want to traverse up or accept it as an argument.
    }

    private String findBasePackage(Path root) throws IOException {
        try (Stream<Path> walk = Files.walk(root.resolve("src/main/java"))) {
            return walk.filter(p -> p.toString().endsWith("Application.java"))
                    .findFirst()
                    .map(p -> {
                        Path rel = root.resolve("src/main/java").relativize(p.getParent());
                        return rel.toString().replace(java.io.File.separatorChar, '.');
                    })
                    .orElseThrow(() -> new IllegalStateException("Base package not found"));
        }
    }

    // ... helper methods remain similar but write to featureDir directly without
    // subfolders like 'domain', 'repository' usually
    // OR we transform structure to:
    // com.example.user
    // User.java
    // UserRepository.java
    // UserService.java
    // UserController.java
    // UserDTO.java
    // UserMapper.java

    private void createEntity(Path dir, String packageName, EnhanceDomainRequest request) throws IOException {
        String entityName = request.entityName();
        StringBuilder fields = new StringBuilder();
        request.fields().forEach(
                (name, type) -> fields.append("    private ").append(type).append(" ").append(name).append(";\n"));

        String content = """
                package %s;

                import jakarta.persistence.Entity;
                import jakarta.persistence.GeneratedValue;
                import jakarta.persistence.GenerationType;
                import jakarta.persistence.Id;
                import lombok.Data;

                @Entity
                @Data
                public class %s {
                    @Id
                    @GeneratedValue(strategy = GenerationType.IDENTITY)
                    private Long id;
                %s
                }
                """.formatted(packageName, entityName, fields.toString());

        Files.writeString(dir.resolve(entityName + ".java"), content);
    }

    private void createRepository(Path dir, String packageName, EnhanceDomainRequest request) throws IOException {
        String entityName = request.entityName();
        StringBuilder methods = new StringBuilder();
        request.repositoryMethods().forEach(method -> methods.append("    ").append(method).append(";\n"));

        String content = """
                package %s;

                import org.springframework.data.jpa.repository.JpaRepository;
                import org.springframework.stereotype.Repository;

                import java.util.List;
                import java.util.Optional;

                @Repository
                public interface %sRepository extends JpaRepository<%s, Long> {
                %s
                }
                """.formatted(packageName, entityName, entityName, methods.toString());

        Files.writeString(dir.resolve(entityName + "Repository.java"), content);
    }

    private void createDto(Path dir, String packageName, EnhanceDomainRequest request) throws IOException {
        String entityName = request.entityName();
        StringBuilder fields = new StringBuilder();
        request.fields().forEach(
                (name, type) -> fields.append("    private ").append(type).append(" ").append(name).append(";\n"));

        String content = """
                package %s;

                import lombok.Data;

                @Data
                public class %sDTO {
                    private Long id;
                %s
                }
                """.formatted(packageName, entityName, fields.toString());

        Files.writeString(dir.resolve(entityName + "DTO.java"), content);
    }

    private void createMapper(Path dir, String packageName, EnhanceDomainRequest request) throws IOException {
        String entityName = request.entityName();
        String content = """
                package %s;

                import org.mapstruct.Mapper;
                import org.mapstruct.factory.Mappers;

                @Mapper
                public interface %sMapper {
                    %sMapper INSTANCE = Mappers.getMapper(%sMapper.class);

                    %sDTO toDTO(%s entity);
                    %s toEntity(%sDTO dto);
                }
                """.formatted(packageName, entityName, entityName, entityName,
                entityName, entityName, entityName, entityName);

        Files.writeString(dir.resolve(entityName + "Mapper.java"), content);
    }

    private void createService(Path dir, String packageName, EnhanceDomainRequest request) throws IOException {
        String entityName = request.entityName();
        String content = """
                package %s;

                import lombok.RequiredArgsConstructor;
                import lombok.extern.slf4j.Slf4j;
                import org.springframework.stereotype.Service;
                import org.springframework.transaction.annotation.Transactional;

                import java.util.List;
                import java.util.stream.Collectors;

                @Service
                @RequiredArgsConstructor
                @Slf4j
                @Transactional(readOnly = true)
                public class %sService {

                    private final %sRepository repository;
                    private final %sMapper mapper = %sMapper.INSTANCE;

                    public List<%sDTO> findAll() {
                        log.info("Entering findAll"); // Manual log as fallback/addition to AOP
                        return repository.findAll().stream()
                                .map(mapper::toDTO)
                                .collect(Collectors.toList());
                    }
                }
                """.formatted(packageName, entityName, entityName, entityName, entityName, entityName);

        Files.writeString(dir.resolve(entityName + "Service.java"), content);
    }

    private void createController(Path dir, String packageName, EnhanceDomainRequest request) throws IOException {
        String entityName = request.entityName();
        String content = """
                package %s;

                import lombok.RequiredArgsConstructor;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;

                import java.util.List;

                @RestController
                @RequestMapping("/api/%s")
                @RequiredArgsConstructor
                public class %sController {

                    private final %sService service;

                    @GetMapping
                    public List<%sDTO> findAll() {
                        return service.findAll();
                    }
                }
                """.formatted(packageName, entityName.toLowerCase() + "s", entityName, entityName, entityName);

        Files.writeString(dir.resolve(entityName + "Controller.java"), content);
    }
}
