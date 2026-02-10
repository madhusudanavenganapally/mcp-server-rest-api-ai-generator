package com.rvvcode.ai.mcp.server.service;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class TestGenerator {

    private static final Logger log = LoggerFactory.getLogger(TestGenerator.class);

    @Tool(name = "generate_tests", description = "Generate JUnit5 + AssertJ tests with Mockito for services and WebMvcTest for controllers")
    public String generateTests(
            @ToolParam(description = "Class name in source code") String className,
            @ToolParam(description = "Source code for the Java class") String sourceCode) {

        log.info("Entering generateTests className={}", className);
        try {
            CompilationUnit unit = StaticJavaParser.parse(sourceCode);
            String packageName = unit.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("com.generated");
            ClassOrInterfaceDeclaration clazz = unit.getClassByName(className)
                    .orElseThrow(() -> new IllegalArgumentException("Class not found in provided sourceCode: " + className));

            String testCode = isController(clazz)
                    ? webMvcTestSource(packageName, clazz)
                    : serviceUnitTestSource(packageName, clazz);

            Path outputPath = Paths.get(System.getProperty("user.dir"), "generated-tests", className + "Test.java");
            Files.createDirectories(outputPath.getParent());
            Files.writeString(outputPath, testCode);

            return "Generated test at " + outputPath + "\n\n" + testCode;
        } catch (Exception ex) {
            log.error("Failed to generate tests", ex);
            return "Failed to generate tests: " + ex.getMessage();
        } finally {
            log.info("Exiting generateTests className={}", className);
        }
    }

    private boolean isController(ClassOrInterfaceDeclaration clazz) {
        return clazz.getAnnotationByName("RestController").isPresent() || clazz.getAnnotationByName("Controller").isPresent();
    }

    private String serviceUnitTestSource(String packageName, ClassOrInterfaceDeclaration clazz) {
        String className = clazz.getNameAsString();
        String mockFields = clazz.getFields().stream()
                .filter(FieldDeclaration::isPrivate)
                .map(field -> "    @Mock\n    private " + field.getElementType() + " " + field.getVariable(0).getNameAsString() + ";")
                .collect(Collectors.joining("\n\n"));

        List<MethodDeclaration> testableMethods = clazz.getMethods().stream().filter(MethodDeclaration::isPublic)
                .toList();

        String methods = testableMethods.stream()
                .map(method -> {
                    String methodName = method.getNameAsString();
                    return """

                            @Test
                            void %s_shouldExecuteHappyPath() {
                                // arrange

                                // act
                                // var result = subject.%s();

                                // assert
                                assertThat(true).isTrue();
                            }
                            """.formatted(methodName, methodName);
                }).collect(Collectors.joining("\n"));

        return """
                package %s;

                import org.junit.jupiter.api.Test;
                import org.junit.jupiter.api.extension.ExtendWith;
                import org.mockito.InjectMocks;
                import org.mockito.Mock;
                import org.mockito.junit.jupiter.MockitoExtension;

                import static org.assertj.core.api.Assertions.assertThat;

                @ExtendWith(MockitoExtension.class)
                class %sTest {

                %s

                    @InjectMocks
                    private %s subject;

                %s
                }
                """.formatted(packageName, className, mockFields, className, methods);
    }

    private String webMvcTestSource(String packageName, ClassOrInterfaceDeclaration clazz) {
        String className = clazz.getNameAsString();
        String endpoint = "/api/" + className.replace("Controller", "").toLowerCase(Locale.ROOT) + "s";
        return """
                package %s;

                import org.junit.jupiter.api.Test;
                import org.springframework.beans.factory.annotation.Autowired;
                import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
                import org.springframework.boot.test.mock.mockito.MockBean;
                import org.springframework.test.web.servlet.MockMvc;

                import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
                import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

                @WebMvcTest(%s.class)
                class %sTest {

                    @Autowired
                    private MockMvc mockMvc;

                    @MockBean
                    private %sService service;

                    @Test
                    void findAll_shouldReturnOk() throws Exception {
                        mockMvc.perform(get("%s"))
                                .andExpect(status().isOk());
                    }
                }
                """.formatted(packageName, className, className, className.replace("Controller", ""), endpoint);
    }
}
