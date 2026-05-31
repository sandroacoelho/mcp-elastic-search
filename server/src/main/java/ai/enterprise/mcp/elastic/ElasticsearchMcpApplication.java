package ai.enterprise.mcp.elastic;

import ai.enterprise.mcp.elastic.config.ElasticsearchProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Read-only Elasticsearch MCP server (ADR-0003). Security controls live in the
 * platform gateway (ADR-0001); this server honors the minimum server safety
 * contract and exposes only the closed, read-only tool surface.
 */
@SpringBootApplication
@EnableConfigurationProperties(ElasticsearchProperties.class)
public class ElasticsearchMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElasticsearchMcpApplication.class, args);
    }
}
