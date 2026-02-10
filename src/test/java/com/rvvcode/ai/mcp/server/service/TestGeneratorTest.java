package com.rvvcode.ai.mcp.server.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TestGeneratorTest {

    private final TestGenerator testGenerator = new TestGenerator();

    @Test
    void generateTests_shouldCreateMockitoStyleTestForServiceClass(@TempDir Path tempDir) throws Exception {
        Path originalUserDir = Path.of(System.getProperty("user.dir"));
        System.setProperty("user.dir", tempDir.toString());
        try {
            String source = """
                    package com.example.customer;

                    import org.springframework.stereotype.Service;

                    @Service
                    public class CustomerService {
                        public String findByEmail() { return \"ok\"; }
                    }
                    """;

            String result = testGenerator.generateTests("CustomerService", source);

            assertThat(result).contains("Generated test at");
            assertThat(result).contains("@ExtendWith(MockitoExtension.class)");
            assertThat(result).contains("assertThat(true).isTrue()");
            assertThat(Files.exists(tempDir.resolve("generated-tests/CustomerServiceTest.java"))).isTrue();
        } finally {
            System.setProperty("user.dir", originalUserDir.toString());
        }
    }

    @Test
    void generateTests_shouldCreateWebMvcTestForControllerClass(@TempDir Path tempDir) throws Exception {
        Path originalUserDir = Path.of(System.getProperty("user.dir"));
        System.setProperty("user.dir", tempDir.toString());
        try {
            String source = """
                    package com.example.customer;

                    import org.springframework.web.bind.annotation.RestController;

                    @RestController
                    public class CustomerController {
                        public void findAll() {}
                    }
                    """;

            String result = testGenerator.generateTests("CustomerController", source);

            assertThat(result).contains("@WebMvcTest(CustomerController.class)");
            assertThat(result).contains("mockMvc.perform(get(\"/api/customers\"))");
            assertThat(Files.exists(tempDir.resolve("generated-tests/CustomerControllerTest.java"))).isTrue();
        } finally {
            System.setProperty("user.dir", originalUserDir.toString());
        }
    }
}
