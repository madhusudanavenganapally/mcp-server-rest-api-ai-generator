package com.rvvcode.ai.mcp.server.service;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class TestGenerator {

    public record GenerateTestsRequest(String className, String sourceCode) {
    }

    public String generate(GenerateTestsRequest request) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(request.sourceCode());

            String packageName = cu.getPackageDeclaration()
                    .map(pd -> pd.getNameAsString())
                    .orElse("com.example");

            ClassOrInterfaceDeclaration clazz = cu.getClassByName(request.className())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Class " + request.className() + " not found in source"));

            boolean isController = clazz.getAnnotationByName("RestController").isPresent()
                    || clazz.getAnnotationByName("Controller").isPresent();

            String testClassName = request.className() + "Test";

            String testMethods = clazz.getMethods().stream()
                    .filter(MethodDeclaration::isPublic)
                    .map(method -> generateTestMethod(method, request.className(), isController))
                    .collect(Collectors.joining("\n"));

            if (isController) {
                return generateWebMvcTest(packageName, testClassName, request.className(), testMethods);
            } else {
                return generateUnitTest(packageName, testClassName, request.className(), testMethods);
            }

        } catch (Exception e) {
            return "Failed to generate tests: " + e.getMessage();
        }
    }

    private String generateTestMethod(MethodDeclaration method, String className, boolean isController) {
        if (method.getNameAsString().equals(className))
            return ""; // Skip constructor logic if any
        if (isController) {
            return """

                    @Test
                    void %sTest() throws Exception {
                        // mockMvc.perform(get("/api/..."))
                        //        .andExpect(status().isOk());
                    }
                    """.formatted(method.getNameAsString());
        }
        return """

                @Test
                void %sTest() {
                    // TODO: Setup mocks
                    // instance.%s(...);
                    // assertThat(...);
                }
                """.formatted(method.getNameAsString(), method.getNameAsString());
    }

    private String generateWebMvcTest(String packageName, String testName, String targetClass, String methods) {
        return """
                package %s;

                import org.junit.jupiter.api.Test;
                import org.springframework.beans.factory.annotation.Autowired;
                import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
                import org.springframework.boot.test.mock.mockito.MockBean;
                import org.springframework.test.web.servlet.MockMvc;
                import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
                import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

                @WebMvcTest(%s.class)
                class %s {

                    @Autowired
                    private MockMvc mockMvc;

                    // TODO: Add @MockBean for dependencies

                    %s
                }
                """.formatted(packageName, targetClass, testName, methods);
    }

    private String generateUnitTest(String packageName, String testName, String targetClass, String methods) {
        return """
                package %s;

                import org.junit.jupiter.api.Test;
                import org.junit.jupiter.api.extension.ExtendWith;
                import org.mockito.InjectMocks;
                import org.mockito.Mock;
                import org.mockito.junit.jupiter.MockitoExtension;
                import static org.assertj.core.api.Assertions.assertThat;

                @ExtendWith(MockitoExtension.class)
                class %s {

                    @InjectMocks
                    private %s instance;

                    // TODO: Add @Mock beans

                    %s
                }
                """.formatted(packageName, testName, targetClass, methods);
    }
}
