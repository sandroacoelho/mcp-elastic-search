package ai.enterprise.mcp.elastic.tool;

import ai.enterprise.mcp.elastic.domain.model.SearchModels.DocumentResult;
import ai.enterprise.mcp.elastic.domain.model.SearchModels.IndexMappings;
import ai.enterprise.mcp.elastic.domain.model.SearchModels.IndexSummary;
import ai.enterprise.mcp.elastic.domain.model.SearchModels.SearchResultPage;
import ai.enterprise.mcp.elastic.service.SearchService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * The closed, read-only MCP tool surface (ADR-0003 §2.2). Each method becomes a
 * callable tool; descriptions are read by the model, so keep them explicit. No
 * tool here mutates, scripts, or escapes the index allowlist.
 */
@Component
public class SearchTools {

    private final SearchService searchService;

    public SearchTools(SearchService searchService) {
        this.searchService = searchService;
    }

    @Tool(description = "List the logical names of the Elasticsearch indices this server can read, with doc counts.")
    public List<IndexSummary> listIndices() {
        return searchService.listIndices();
    }

    @Tool(description = "Describe an index: its field mappings and relevant settings.")
    public IndexMappings describeIndex(
            @ToolParam(description = "Logical index name from listIndices") String index) {
        return searchService.describeIndex(index);
    }

    @Tool(description = "Run a READ-ONLY Elasticsearch search and return a bounded page of hits. "
            + "The body accepts only query/sort/search_after/_source/track_total_hits/min_score; "
            + "scripting is rejected. Page size is capped by the server limit.")
    public SearchResultPage search(
            @ToolParam(description = "Logical index name from listIndices") String index,
            @ToolParam(description = "Elasticsearch search body (query DSL); scripting is not allowed", required = false)
            Map<String, Object> body,
            @ToolParam(description = "Max hits to return (capped by server limit)", required = false) Integer size,
            @ToolParam(description = "Offset for pagination, within max_result_window", required = false) Integer from,
            @ToolParam(description = "Fields to include in _source; empty returns the allowed default", required = false)
            List<String> sourceFields) {
        return searchService.search(index, body, size, from, sourceFields);
    }

    @Tool(description = "Count documents matching a READ-ONLY query. Scripting is rejected.")
    public long count(
            @ToolParam(description = "Logical index name from listIndices") String index,
            @ToolParam(description = "Elasticsearch query body; scripting is not allowed", required = false)
            Map<String, Object> body) {
        return searchService.count(index, body);
    }

    @Tool(description = "Fetch a single document by id from an allowlisted index.")
    public DocumentResult getDocument(
            @ToolParam(description = "Logical index name from listIndices") String index,
            @ToolParam(description = "Document id") String id,
            @ToolParam(description = "Fields to include in _source; empty returns the allowed default", required = false)
            List<String> sourceFields) {
        return searchService.getDocument(index, id, sourceFields);
    }
}
