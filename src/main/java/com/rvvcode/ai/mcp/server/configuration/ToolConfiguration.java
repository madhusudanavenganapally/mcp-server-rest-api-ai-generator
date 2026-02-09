package com.rvvcode.ai.mcp.server.configuration;

import com.rvvcode.ai.mcp.server.service.ProjectGenerator;
import com.rvvcode.ai.mcp.server.service.DomainEnhancer;
import com.rvvcode.ai.mcp.server.service.TestGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.function.Function;
import org.springframework.context.annotation.Description;

@Configuration
public class ToolConfiguration {

    @Bean
    @Description("Generate a production-ready Spring Boot 3 project skeleton")
    public Function<ProjectGenerator.BootstrapProjectRequest, String> bootstrapProject(
            ProjectGenerator projectGenerator) {
        return projectGenerator::bootstrap;
    }

    @Bean
    @Description("Enhance an existing project with Entity, Repository, and Mapper")
    public Function<DomainEnhancer.EnhanceDomainRequest, String> enhanceDomain(DomainEnhancer domainEnhancer) {
        return domainEnhancer::enhance;
    }

    @Bean
    @Description("Generate JUnit 5 and AssertJ test classes for a given source class")
    public Function<TestGenerator.GenerateTestsRequest, String> generateTests(TestGenerator testGenerator) {
        return testGenerator::generate;
    }
}
