package ai.enterprise.mcp.elastic.port;

import ai.enterprise.mcp.elastic.domain.model.SearchModels.DocumentResult;
import ai.enterprise.mcp.elastic.domain.model.SearchModels.Hit;
import ai.enterprise.mcp.elastic.domain.model.SearchModels.IndexMappings;

import java.util.List;
import java.util.Map;

/**
 * Outbound port to the search backend. The domain/service layers depend only on
 * this interface; the single {@code ..adapter..} implementation talks to the real
 * Elasticsearch client (ArchUnit rules A1/A2). All index names crossing this port
 * are already-resolved <em>concrete</em> names — scoping/guarding happens above it.
 */
public interface SearchPort {

    /** All concrete indices the server's credential can see, with doc counts. */
    List<ConcreteIndex> listIndices();

    /** Mappings + settings for a concrete index. */
    IndexMappings describeIndex(String concreteIndex);

    /** A bounded page of hits for a guarded search body. */
    SearchPage search(String concreteIndex, Map<String, Object> body, int size, int from, List<String> sourceFields);

    /** Match count for a guarded query body. */
    long count(String concreteIndex, Map<String, Object> body);

    /** A single document by id. */
    DocumentResult getDocument(String concreteIndex, String id, List<String> sourceFields);

    /** A concrete index name plus its document count, as reported by the backend. */
    record ConcreteIndex(String name, long docCount) {
    }

    /** Backend-shaped page: hits plus the (possibly lower-bounded) total. */
    record SearchPage(long totalHits, boolean totalIsLowerBound, List<Hit> hits) {
    }
}
