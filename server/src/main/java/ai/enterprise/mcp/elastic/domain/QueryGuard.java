package ai.enterprise.mcp.elastic.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Read-only validator for Elasticsearch search bodies — the Elasticsearch analogue
 * of the database server's {@code SqlGuard}. It accepts only a known-safe set of
 * top-level search keys and rejects every scripting construct anywhere in the body
 * (Painless = code execution), mitigating query-DSL injection / scripting RCE
 * (ADR-0003 threat E1).
 *
 * <p>This is the application-layer guard; the Elasticsearch role granted to the
 * server must independently be read-only (least privilege) as the authoritative
 * control.
 */
public class QueryGuard {

    /** Top-level keys a caller may include in a search body. Everything else is rejected. */
    private static final Set<String> ALLOWED_TOP_LEVEL_KEYS = Set.of(
            "query", "sort", "search_after", "_source", "track_total_hits", "min_score");

    /** Keys that introduce server-side scripting / code execution — banned at any depth. */
    private static final Set<String> BANNED_KEYS = Set.of(
            "script", "script_score", "script_fields", "scripted_metric", "runtime_mappings");

    /**
     * Validate a search/count body. {@code null} or empty is treated as a
     * match-all and accepted. Throws {@link QueryNotAllowedException} otherwise.
     */
    public void assertReadOnlySearchBody(Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            return;
        }
        for (String key : body.keySet()) {
            if (!ALLOWED_TOP_LEVEL_KEYS.contains(key)) {
                throw new QueryNotAllowedException(
                        "Unsupported search key '" + key + "'. Allowed: " + ALLOWED_TOP_LEVEL_KEYS);
            }
        }
        assertNoScripting(body);
    }

    private void assertNoScripting(Object node) {
        switch (node) {
            case Map<?, ?> map -> {
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    if (e.getKey() instanceof String k && BANNED_KEYS.contains(k)) {
                        throw new QueryNotAllowedException(
                                "Scripting construct '" + k + "' is not permitted on a read-only server");
                    }
                    assertNoScripting(e.getValue());
                }
            }
            case List<?> list -> {
                for (Object item : list) {
                    assertNoScripting(item);
                }
            }
            default -> {
                // scalar — nothing to inspect
            }
        }
    }
}
