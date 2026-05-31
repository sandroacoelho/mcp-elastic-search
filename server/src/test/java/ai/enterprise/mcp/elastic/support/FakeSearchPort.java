package ai.enterprise.mcp.elastic.support;

import ai.enterprise.mcp.elastic.domain.model.SearchModels.DocumentResult;
import ai.enterprise.mcp.elastic.domain.model.SearchModels.Hit;
import ai.enterprise.mcp.elastic.domain.model.SearchModels.IndexMappings;
import ai.enterprise.mcp.elastic.port.SearchPort;

import java.util.List;
import java.util.Map;

/**
 * Hand-written {@link SearchPort} double for unit tests — records the bounds it was
 * called with so tests can assert clamping/guarding behaviour without a live cluster.
 */
public class FakeSearchPort implements SearchPort {

    public List<ConcreteIndex> indices = List.of();
    public boolean documentFound = true;

    public int lastSize = -1;
    public int lastFrom = -1;
    public String lastConcreteIndex;
    public Map<String, Object> lastBody;

    @Override
    public List<ConcreteIndex> listIndices() {
        return indices;
    }

    @Override
    public IndexMappings describeIndex(String concreteIndex) {
        this.lastConcreteIndex = concreteIndex;
        return new IndexMappings(concreteIndex, Map.of("properties", Map.of()), Map.of("number_of_shards", "1"));
    }

    @Override
    public SearchPage search(String concreteIndex, Map<String, Object> body, int size, int from,
                             List<String> sourceFields) {
        this.lastConcreteIndex = concreteIndex;
        this.lastBody = body;
        this.lastSize = size;
        this.lastFrom = from;
        return new SearchPage(2, false, List.of(new Hit("a", 1.0, Map.of("k", "v"))));
    }

    @Override
    public long count(String concreteIndex, Map<String, Object> body) {
        this.lastConcreteIndex = concreteIndex;
        this.lastBody = body;
        return 42L;
    }

    @Override
    public DocumentResult getDocument(String concreteIndex, String id, List<String> sourceFields) {
        this.lastConcreteIndex = concreteIndex;
        return new DocumentResult(concreteIndex, id, documentFound,
                documentFound ? Map.of("k", "v") : Map.of());
    }
}
