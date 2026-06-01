package ai.enterprise.mcp.elastic.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the application-wide Jackson 2 {@link ObjectMapper} used by the adapter
 * layer to read and write Elasticsearch JSON.
 *
 * <p>Spring AI's MCP server starter (1.1.x) registers its own
 * {@code mcpServerObjectMapper} with {@code defaultCandidate = false}, so it is
 * present in the context but ineligible for by-type autowiring. Its mere presence
 * makes Spring Boot's {@code JacksonAutoConfiguration} back off (its mapper is
 * {@code @ConditionalOnMissingBean(ObjectMapper.class)}), leaving no injectable
 * {@code ObjectMapper} for our beans. Declaring one here restores a single
 * injectable candidate while the MCP server keeps using its own tuned mapper.
 *
 * <p>Spring Boot 4 defaults to Jackson 3 and no longer auto-registers a Jackson 2
 * {@code Jackson2ObjectMapperBuilder}, so the mapper is built directly here.
 * {@code findAndAddModules()} discovers Jackson 2 modules via the service loader,
 * matching the registration the framework would otherwise do.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .findAndAddModules()
                .build();
    }
}
