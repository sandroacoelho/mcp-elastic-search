package ai.enterprise.mcp.elastic.service;

import ai.enterprise.mcp.elastic.domain.IndexAllowlist;
import ai.enterprise.mcp.elastic.domain.QueryGuard;
import ai.enterprise.mcp.elastic.domain.QueryNotAllowedException;
import ai.enterprise.mcp.elastic.domain.model.SearchLimits;
import ai.enterprise.mcp.elastic.domain.model.SearchModels.DocumentResult;
import ai.enterprise.mcp.elastic.domain.model.SearchModels.IndexMappings;
import ai.enterprise.mcp.elastic.domain.model.SearchModels.IndexSummary;
import ai.enterprise.mcp.elastic.domain.model.SearchModels.SearchResultPage;
import ai.enterprise.mcp.elastic.port.SearchPort;

import java.util.List;
import java.util.Map;

/**
 * Read-only search orchestration: guard the query, resolve and scope the index to
 * the allowlist, clamp result bounds, then delegate to the {@link SearchPort}.
 * All security-relevant decisions happen here, above the backend adapter.
 */
public class SearchService {

    private final SearchPort port;
    private final QueryGuard guard;
    private final IndexAllowlist allowlist;
    private final SearchLimits limits;

    public SearchService(SearchPort port, QueryGuard guard, IndexAllowlist allowlist, SearchLimits limits) {
        this.port = port;
        this.guard = guard;
        this.allowlist = allowlist;
        this.limits = limits;
    }

    /** Logical indices the caller may read, filtered to the allowlist. */
    public List<IndexSummary> listIndices() {
        return port.listIndices().stream()
                .filter(ci -> allowlist.isAllowedConcrete(ci.name()))
                .map(ci -> new IndexSummary(
                        allowlist.logicalFor(ci.name()).orElse(ci.name()),
                        ci.name(),
                        ci.docCount()))
                .toList();
    }

    /** Mappings + settings for an allowlisted index. */
    public IndexMappings describeIndex(String logicalName) {
        String concrete = allowlist.resolve(logicalName);
        IndexMappings raw = port.describeIndex(concrete);
        return new IndexMappings(logicalName, raw.mappings(), raw.settings());
    }

    /** A bounded, guarded page of search hits. */
    public SearchResultPage search(String logicalName, Map<String, Object> body, Integer size, Integer from,
                                   List<String> sourceFields) {
        guard.assertReadOnlySearchBody(body);
        String concrete = allowlist.resolve(logicalName);
        int effSize = clampSize(size);
        int effFrom = clampFrom(from);
        if ((long) effFrom + effSize > limits.maxResultWindow()) {
            throw new QueryNotAllowedException(
                    "Pagination beyond max_result_window (" + limits.maxResultWindow()
                            + ") is not allowed; narrow the query or use search_after");
        }
        List<String> effectiveSourceFields = allowlist.sourceFieldsFor(logicalName, sourceFields);
        SearchPort.SearchPage page = port.search(concrete, body, effSize, effFrom, effectiveSourceFields);
        return new SearchResultPage(logicalName, page.totalHits(), page.totalIsLowerBound(), page.hits());
    }

    /** Guarded match count for an allowlisted index. */
    public long count(String logicalName, Map<String, Object> body) {
        guard.assertReadOnlySearchBody(body);
        String concrete = allowlist.resolve(logicalName);
        return port.count(concrete, body);
    }

    /** A single document by id from an allowlisted index. */
    public DocumentResult getDocument(String logicalName, String id, List<String> sourceFields) {
        if (id == null || id.isBlank()) {
            throw new QueryNotAllowedException("Document id must not be empty");
        }
        String concrete = allowlist.resolve(logicalName);
        List<String> effectiveSourceFields = allowlist.sourceFieldsFor(logicalName, sourceFields);
        DocumentResult raw = port.getDocument(concrete, id, effectiveSourceFields);
        return new DocumentResult(logicalName, id, raw.found(), raw.source());
    }

    private int clampSize(Integer requested) {
        int size = (requested == null || requested <= 0) ? limits.defaultPageSize() : requested;
        return Math.min(size, limits.maxPageSize());
    }

    private int clampFrom(Integer requested) {
        return (requested == null || requested < 0) ? 0 : requested;
    }
}
