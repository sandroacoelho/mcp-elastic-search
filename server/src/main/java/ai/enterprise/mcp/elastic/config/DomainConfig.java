package ai.enterprise.mcp.elastic.config;

import ai.enterprise.mcp.elastic.domain.IndexAllowlist;
import ai.enterprise.mcp.elastic.domain.QueryGuard;
import ai.enterprise.mcp.elastic.domain.model.SearchLimits;
import ai.enterprise.mcp.elastic.port.SearchPort;
import ai.enterprise.mcp.elastic.service.SearchService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the framework-free domain/service beans from configuration. Keeping this
 * out of the domain classes themselves lets them be constructed directly in unit
 * tests (supporting the coverage gate) and keeps Spring annotations off the
 * security-critical logic.
 */
@Configuration
public class DomainConfig {

    @Bean
    public QueryGuard queryGuard() {
        return new QueryGuard();
    }

    @Bean
    public IndexAllowlist indexAllowlist(ElasticsearchProperties props) {
        return new IndexAllowlist(props.getAllowlist());
    }

    @Bean
    public SearchLimits searchLimits(ElasticsearchProperties props) {
        ElasticsearchProperties.Limits l = props.getLimits();
        return new SearchLimits(
                l.getDefaultPageSize(), l.getMaxPageSize(), l.getMaxResultWindow(), l.getRequestTimeoutSeconds());
    }

    @Bean
    public SearchService searchService(SearchPort port, QueryGuard guard, IndexAllowlist allowlist,
                                       SearchLimits limits) {
        return new SearchService(port, guard, allowlist, limits);
    }
}
