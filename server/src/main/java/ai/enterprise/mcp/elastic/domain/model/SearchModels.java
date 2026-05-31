package ai.enterprise.mcp.elastic.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Immutable result records returned by the read-only tool surface (ADR-0003 §2.2).
 * These carry only projected, gateway-shaped data — never the raw Elasticsearch
 * response object (enforced by ArchUnit rule A4).
 */
public final class SearchModels {

    private SearchModels() {
    }

    /** One allowlisted index the caller may read, with its visible document count. */
    public record IndexSummary(String logicalName, String concreteIndex, long docCount) {
    }

    /** Mappings + relevant settings for an index (no secrets). */
    public record IndexMappings(String logicalName, Map<String, Object> mappings, Map<String, Object> settings) {
    }

    /** A single hit with its (projected) source. */
    public record Hit(String id, double score, Map<String, Object> source) {
    }

    /** A bounded page of search hits. */
    public record SearchResultPage(String logicalName, long totalHits, boolean totalIsLowerBound, List<Hit> hits) {
    }

    /** A single document fetched by id, or {@code found=false} when absent. */
    public record DocumentResult(String logicalName, String id, boolean found, Map<String, Object> source) {
    }
}
