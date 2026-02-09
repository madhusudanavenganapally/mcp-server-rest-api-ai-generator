# Spring AI MCP Server

This is a High-Performance MCP Server built with Spring Boot 3 and Spring AI.

## Tools Explicitly Exposed

1.  **bootstrap_project**: Generates a production-ready Spring Boot 3 project skeleton.
    -   *Inputs*: `projectName`, `basePackage`, `apiRequirements`, `specFilePath` (optional JSON config).
    -   *Output*: Project directory with `build.gradle` (Gradle), `settings.gradle`, `application.yml`, and structure.

2.  **enhance_domain**: Enhances an existing project with domains.
    -   *Inputs*: `entityName`, `fields` (Map<Name, Type>), `repositoryMethods` (List<String>)
    -   *Output*: Generates Entity, Repository, DTO, Mapper, Service (with AOP+manual logging), and Controller within a **Package-by-Feature** structure (e.g., `com.example.user` package containing all User-related classes).

3.  **generate_tests**: Generates unit and integration tests.
    -   *Inputs*: `className`, `sourceCode`
    -   *Output*: 
        -   **Service**: JUnit 5 + Mockito test class.
        -   **Controller**: `@WebMvcTest` with MockMvc.
        -   Targets >80% coverage template (heuristic based).

## Prerequisites

-   Java 17+
-   Gradle (wrapper not included, use local installation or generate wrapper)

## How to Run

1.  Build the project:
    ```bash
    gradle build
    ```

2.  Run the server:
    ```bash
    java -jar build/libs/mcp-server-rest-api-ai-generator-0.0.1-SNAPSHOT.jar
    ```

The server uses `spring-ai-mcp-server-spring-boot-starter` which automatically exposes the tools defined as beans in `ToolConfiguration`.

## Docker Support

You can run the MCP Server as a Docker container.

1.  **Build the Docker Image**:
    ```bash
    docker build -t mcp-server-rest-api-ai-generator .
    ```

2.  **Run the Container**:
    > **Important**: Since this server generates files (bootstrap_project, enhance_domain), you MUST mount a local directory to the container to see the generated output.

    ```bash
    # Run mapping current directory to /workspace inside container
    docker run -it --rm -p 8080:8080 -v ${PWD}:/workspace mcp-server-rest-api-ai-generator
    ```

    -   The server runs on port `8080`.
    -   Generated projects will appear in your current directory.

