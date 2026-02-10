package com.rvvcode.ai.mcp.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DomainEnhancer {

    private static final Logger log = LoggerFactory.getLogger(DomainEnhancer.class);

    @Tool(name = "enhance_domain", description = "Create or update @Entity, MapStruct mapper, repository methods, service, and controller using package-by-feature")
    public String enhanceDomain(
            @ToolParam(description = "Entity name, e.g. Customer") String entityName,
            @ToolParam(description = "Field map in format fieldName:JavaType") Map<String, String> fields,
            @ToolParam(description = "Repository methods signatures, e.g. Optional<CustomerEntity> findByEmail(String email)") List<String> repositoryMethods) {
        log.info("Entering enhanceDomain entityName={}", entityName);
        try {
            Path projectRoot = Paths.get(System.getProperty("user.dir"));
            String basePackage = resolveBasePackage(projectRoot);
            String featureName = entityName.toLowerCase();

            Path featureDir = projectRoot.resolve("src/main/java")
                    .resolve(basePackage.replace('.', '/'))
                    .resolve(featureName);
            Files.createDirectories(featureDir);

            writeFile(featureDir.resolve(entityName + "Entity.java"), entityContent(basePackage, featureName, entityName, fields));
            writeFile(featureDir.resolve(entityName + "Dto.java"), dtoContent(basePackage, featureName, entityName, fields));
            writeFile(featureDir.resolve(entityName + "Mapper.java"), mapperContent(basePackage, featureName, entityName));
            writeFile(featureDir.resolve(entityName + "Repository.java"), repositoryContent(basePackage, featureName, entityName, repositoryMethods));
            writeFile(featureDir.resolve(entityName + "Service.java"), serviceContent(basePackage, featureName, entityName));
            writeFile(featureDir.resolve(entityName + "Controller.java"), controllerContent(basePackage, featureName, entityName));

            return "Enhanced domain for " + entityName + " at " + featureDir;
        } catch (Exception ex) {
            log.error("Failed to enhance domain", ex);
            return "Failed to enhance domain: " + ex.getMessage();
        } finally {
            log.info("Exiting enhanceDomain entityName={}", entityName);
        }
    }

    private void writeFile(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }

    private String resolveBasePackage(Path projectRoot) throws IOException {
        Path javaRoot = projectRoot.resolve("src/main/java");
        try (var walk = Files.walk(javaRoot)) {
            return walk.filter(path -> path.getFileName().toString().endsWith("Application.java"))
                    .findFirst()
                    .map(path -> javaRoot.relativize(path.getParent()).toString().replace('/', '.'))
                    .orElseThrow(() -> new IllegalStateException("Cannot resolve base package from Application class"));
        }
    }

    private String entityContent(String basePackage, String featureName, String entityName, Map<String, String> fields) {
        String fieldText = fields.entrySet().stream()
                .map(entry -> "    private " + entry.getValue() + " " + entry.getKey() + ";")
                .collect(Collectors.joining("\n"));
        return """
                package %s.%s;

                import jakarta.persistence.*;

                @Entity
                @Table(name = "%s")
                public class %sEntity {

                    @Id
                    @GeneratedValue(strategy = GenerationType.IDENTITY)
                    private Long id;

                %s

                    public Long getId() { return id; }
                    public void setId(Long id) { this.id = id; }
                }
                """.formatted(basePackage, featureName, entityName.toUpperCase(), entityName, fieldText);
    }

    private String dtoContent(String basePackage, String featureName, String entityName, Map<String, String> fields) {
        String fieldText = fields.entrySet().stream()
                .map(entry -> "    private " + entry.getValue() + " " + entry.getKey() + ";")
                .collect(Collectors.joining("\n"));
        return """
                package %s.%s;

                public class %sDto {

                    private Long id;
                %s

                    public Long getId() { return id; }
                    public void setId(Long id) { this.id = id; }
                }
                """.formatted(basePackage, featureName, entityName, fieldText);
    }

    private String mapperContent(String basePackage, String featureName, String entityName) {
        return """
                package %s.%s;

                import org.mapstruct.Mapper;
                import org.mapstruct.ReportingPolicy;

                @Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
                public interface %sMapper {

                    %sDto toDto(%sEntity entity);

                    %sEntity toEntity(%sDto dto);
                }
                """.formatted(basePackage, featureName, entityName, entityName, entityName, entityName, entityName);
    }

    private String repositoryContent(String basePackage, String featureName, String entityName, List<String> methods) {
        String methodText = methods == null ? "" : methods.stream().map(m -> "    " + m + ";").collect(Collectors.joining("\n"));
        return """
                package %s.%s;

                import org.springframework.data.jpa.repository.JpaRepository;

                public interface %sRepository extends JpaRepository<%sEntity, Long> {
                %s
                }
                """.formatted(basePackage, featureName, entityName, entityName, methodText);
    }

    private String serviceContent(String basePackage, String featureName, String entityName) {
        return """
                package %s.%s;

                import org.slf4j.Logger;
                import org.slf4j.LoggerFactory;
                import org.springframework.stereotype.Service;
                import org.springframework.transaction.annotation.Transactional;

                import java.util.List;

                @Service
                @Transactional
                public class %sService {

                    private static final Logger log = LoggerFactory.getLogger(%sService.class);
                    private final %sRepository repository;
                    private final %sMapper mapper;

                    public %sService(%sRepository repository, %sMapper mapper) {
                        this.repository = repository;
                        this.mapper = mapper;
                    }

                    @Transactional(readOnly = true)
                    public List<%sDto> findAll() {
                        log.info("Entering %sService.findAll");
                        List<%sDto> result = repository.findAll().stream().map(mapper::toDto).toList();
                        log.info("Exiting %sService.findAll size={}", result.size());
                        return result;
                    }
                }
                """.formatted(basePackage, featureName, entityName, entityName, entityName, entityName, entityName,
                entityName, entityName, entityName, entityName, entityName, entityName);
    }

    private String controllerContent(String basePackage, String featureName, String entityName) {
        return """
                package %s.%s;

                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;

                import java.util.List;

                @RestController
                @RequestMapping("/api/%ss")
                public class %sController {

                    private final %sService service;

                    public %sController(%sService service) {
                        this.service = service;
                    }

                    @GetMapping
                    public List<%sDto> findAll() {
                        return service.findAll();
                    }
                }
                """.formatted(basePackage, featureName, featureName, entityName, entityName, entityName, entityName);
    }
}
