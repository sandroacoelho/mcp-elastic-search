package ai.enterprise.mcp.elastic.config;

import ai.enterprise.mcp.elastic.tool.SearchTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the {@link SearchTools} methods as MCP tool callbacks so the Spring AI
 * MCP server starter advertises them over HTTP/SSE.
 */
@Configuration
public class ToolConfig {

    @Bean
    public ToolCallbackProvider searchToolCallbacks(SearchTools searchTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(searchTools)
                .build();
    }
}
