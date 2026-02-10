package com.rvvcode.ai.mcp.server.configuration;

import com.rvvcode.ai.mcp.server.service.DomainEnhancer;
import com.rvvcode.ai.mcp.server.service.ProjectGenerator;
import com.rvvcode.ai.mcp.server.service.TestGenerator;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolConfiguration {

    @Bean
    ToolCallbackProvider toolCallbackProvider(
            ProjectGenerator projectGenerator,
            DomainEnhancer domainEnhancer,
            TestGenerator testGenerator) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(projectGenerator, domainEnhancer, testGenerator)
                .build();
    }
}
