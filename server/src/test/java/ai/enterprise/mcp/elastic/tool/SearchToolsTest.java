package ai.enterprise.mcp.elastic.tool;

import ai.enterprise.mcp.elastic.domain.IndexAllowlist;
import ai.enterprise.mcp.elastic.domain.QueryGuard;
import ai.enterprise.mcp.elastic.domain.model.SearchLimits;
import ai.enterprise.mcp.elastic.port.SearchPort;
import ai.enterprise.mcp.elastic.service.SearchService;
import ai.enterprise.mcp.elastic.support.FakeSearchPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the MCP tool surface delegates to the service. Backed by a real
 * {@link SearchService} over a fake port so the closed tool set is exercised end
 * to end without a live cluster.
 */
class SearchToolsTest {

    private FakeSearchPort port;
    private SearchTools tools;

    @BeforeEach
    void setUp() {
        port = new FakeSearchPort();
        IndexAllowlist allowlist = new IndexAllowlist(Map.of("customers", "customers-v3"));
        SearchService service = new SearchService(
                port, new QueryGuard(), allowlist, new SearchLimits(10, 100, 10_000, 10));
        tools = new SearchTools(service);
    }

    @Test
    void listIndicesDelegates() {
        port.indices = List.of(new SearchPort.ConcreteIndex("customers-v3", 7));
        assertThat(tools.listIndices())
                .singleElement()
                .satisfies(s -> {
                    assertThat(s.logicalName()).isEqualTo("customers");
                    assertThat(s.docCount()).isEqualTo(7);
                });
    }

    @Test
    void describeIndexDelegates() {
        assertThat(tools.describeIndex("customers").logicalName()).isEqualTo("customers");
    }

    @Test
    void searchDelegates() {
        var page = tools.search("customers", Map.of("query", Map.of("match_all", Map.of())), 5, 0, List.of("k"));
        assertThat(page.hits()).hasSize(1);
        assertThat(port.lastSize).isEqualTo(5);
    }

    @Test
    void countDelegates() {
        assertThat(tools.count("customers", Map.of())).isEqualTo(42L);
    }

    @Test
    void getDocumentDelegates() {
        assertThat(tools.getDocument("customers", "1", null).found()).isTrue();
    }
}
