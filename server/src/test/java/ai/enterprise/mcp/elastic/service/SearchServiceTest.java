package ai.enterprise.mcp.elastic.service;

import ai.enterprise.mcp.elastic.domain.IndexAllowlist;
import ai.enterprise.mcp.elastic.domain.QueryGuard;
import ai.enterprise.mcp.elastic.domain.QueryNotAllowedException;
import ai.enterprise.mcp.elastic.domain.model.SearchLimits;
import ai.enterprise.mcp.elastic.domain.model.SearchModels.IndexSummary;
import ai.enterprise.mcp.elastic.domain.model.SearchModels.SearchResultPage;
import ai.enterprise.mcp.elastic.port.SearchPort;
import ai.enterprise.mcp.elastic.support.FakeSearchPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SearchServiceTest {

    private FakeSearchPort port;
    private SearchService service;

    @BeforeEach
    void setUp() {
        port = new FakeSearchPort();
        Map<String, String> allow = new LinkedHashMap<>();
        allow.put("customers", "customers-v3");
        allow.put("secret", ".internal");
        IndexAllowlist allowlist = new IndexAllowlist(allow, Map.of("customers", List.of("id", "name")));
        SearchLimits limits = new SearchLimits(10, 100, 10_000, 10);
        service = new SearchService(port, new QueryGuard(), allowlist, limits);
    }

    @Test
    void listIndicesFiltersToAllowlistAndMapsLogicalNames() {
        port.indices = List.of(
                new SearchPort.ConcreteIndex("customers-v3", 5),
                new SearchPort.ConcreteIndex("other-index", 9),
                new SearchPort.ConcreteIndex(".internal", 1));

        List<IndexSummary> result = service.listIndices();

        assertThat(result).containsExactly(new IndexSummary("customers", "customers-v3", 5));
    }

    @Test
    void describeIndexResolvesAndStampsLogicalName() {
        var mappings = service.describeIndex("customers");
        assertThat(mappings.logicalName()).isEqualTo("customers");
        assertThat(port.lastConcreteIndex).isEqualTo("customers-v3");
    }

    @Test
    void describeUnknownIndexFailsClosed() {
        assertThatThrownBy(() -> service.describeIndex("orders"))
                .isInstanceOf(QueryNotAllowedException.class);
    }

    @Test
    void searchAppliesDefaultsWhenSizeAndFromOmitted() {
        SearchResultPage page = service.search("customers", Map.of("query", Map.of("match_all", Map.of())),
                null, null, null);

        assertThat(page.logicalName()).isEqualTo("customers");
        assertThat(page.totalHits()).isEqualTo(2);
        assertThat(page.hits()).hasSize(1);
        assertThat(port.lastSize).isEqualTo(10);   // default
        assertThat(port.lastFrom).isEqualTo(0);     // default
        assertThat(port.lastConcreteIndex).isEqualTo("customers-v3");
    }

    @Test
    void searchClampsSizeToMaxAndNegativeFromToZero() {
        service.search("customers", null, 500, -3, List.of("name"));
        assertThat(port.lastSize).isEqualTo(100);   // capped at maxPageSize
        assertThat(port.lastFrom).isEqualTo(0);
        assertThat(port.lastSourceFields).containsExactly("name");
    }

    @Test
    void searchZeroSizeUsesDefault() {
        service.search("customers", null, 0, 5, null);
        assertThat(port.lastSize).isEqualTo(10);
        assertThat(port.lastFrom).isEqualTo(5);
    }

    @Test
    void searchRejectsDeepPagination() {
        assertThatThrownBy(() -> service.search("customers", null, 100, 9_999, null))
                .isInstanceOf(QueryNotAllowedException.class)
                .hasMessageContaining("max_result_window");
    }

    @Test
    void searchRejectsDisallowedSourceFields() {
        assertThatThrownBy(() -> service.search("customers", null, 10, 0, List.of("ssn")))
                .isInstanceOf(QueryNotAllowedException.class)
                .hasMessageContaining("Source field");
    }

    @Test
    void searchRejectsScriptingBeforeHittingPort() {
        Map<String, Object> body = Map.of("query", Map.of("script_score", Map.of("script", "x")));
        assertThatThrownBy(() -> service.search("customers", body, null, null, null))
                .isInstanceOf(QueryNotAllowedException.class);
        assertThat(port.lastConcreteIndex).isNull(); // never reached the port
    }

    @Test
    void countGuardsAndDelegates() {
        long n = service.count("customers", Map.of("query", Map.of("match_all", Map.of())));
        assertThat(n).isEqualTo(42L);
    }

    @Test
    void countRejectsScripting() {
        assertThatThrownBy(() -> service.count("customers", Map.of("script", "x")))
                .isInstanceOf(QueryNotAllowedException.class);
    }

    @Test
    void getDocumentReturnsFoundDocument() {
        var doc = service.getDocument("customers", "42", List.of("name"));
        assertThat(doc.found()).isTrue();
        assertThat(doc.logicalName()).isEqualTo("customers");
        assertThat(doc.id()).isEqualTo("42");
    }

    @Test
    void getDocumentMissingReturnsNotFound() {
        port.documentFound = false;
        var doc = service.getDocument("customers", "404", null);
        assertThat(doc.found()).isFalse();
    }

    @Test
    void getDocumentRejectsBlankId() {
        assertThatThrownBy(() -> service.getDocument("customers", "  ", null))
                .isInstanceOf(QueryNotAllowedException.class)
                .hasMessageContaining("id must not be empty");
    }
}
