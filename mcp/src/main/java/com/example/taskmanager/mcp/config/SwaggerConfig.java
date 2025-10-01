package com.example.taskmanager.mcp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

//@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Task Manager MCP Server API")
                        .version("1.0.0")
                        .description("**AI-Powered Task Management MCP Server**\n\n" +
                                "This API serves as a Model Context Protocol (MCP) server that allows AI agents " +
                                "(such as Claude, GPT-4, etc.) to interact with a task management database.\n\n" +
                                "## Key Features:\n" +
                                "- **Schema Inspection**: AI agents can understand database structure\n" +
                                "- **Bulk Data Operations**: Insert thousands of tasks efficiently\n" +
                                "- **Analytics & Insights**: Get comprehensive task statistics\n" +
                                "- **AI-Optimized**: Designed specifically for AI agent interaction\n\n" +
                                "## Typical AI Workflow:\n" +
                                "1. **GET /mcp/help** - Understand available operations\n" +
                                "2. **GET /mcp/schema/tasks** - Inspect database schema\n" +
                                "3. **POST /mcp/tasks** - Insert generated task data\n" +
                                "4. **GET /mcp/tasks/summary** - Verify and analyze results\n\n" +
                                "## Task Statuses:\n" +
                                "- `TODO`: Task is pending\n" +
                                "- `IN_PROGRESS`: Task is being worked on\n" +
                                "- `DONE`: Task is completed\n" +
                                "- `CANCELLED`: Task was cancelled\n" +
                                "- `ON_HOLD`: Task is temporarily paused\n\n" +
                                "## Priority Levels:\n" +
                                "- `LOW`: Low priority task\n" +
                                "- `MEDIUM`: Medium priority task\n" +
                                "- `HIGH`: High priority task\n" +
                                "- `URGENT`: Urgent priority task")
                        .contact(new Contact()
                                .name("MCP Development Team")
                                .email("mcp-dev@example.com")
                                .url("https://github.com/your-repo"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development Server"),
                        new Server()
                                .url("https://your-production-url.com")
                                .description("Production Server")
                ));
    }
}