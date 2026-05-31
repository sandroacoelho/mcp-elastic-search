package ai.enterprise.mcp.elastic.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Maps caller-facing <em>logical</em> index names to concrete Elasticsearch
 * indices/aliases, and refuses anything outside the configured set. Unlisted names
 * and {@code .}-prefixed system/internal indices fail closed — mitigating
 * index/scope widening (ADR-0003 threat E2).
 */
public class IndexAllowlist {

    private final Map<String, String> logicalToConcrete;

    public IndexAllowlist(Map<String, String> logicalToConcrete) {
        // Defensive copy; preserve insertion order for stable listings.
        this.logicalToConcrete = new LinkedHashMap<>(logicalToConcrete == null ? Map.of() : logicalToConcrete);
    }

    /** Logical names the caller may reference. */
    public Set<String> logicalNames() {
        return Set.copyOf(logicalToConcrete.keySet());
    }

    /**
     * Resolve a logical name to its concrete index, failing closed if the name is
     * unknown or the target looks like a system index.
     */
    public String resolve(String logicalName) {
        if (logicalName == null || logicalName.isBlank()) {
            throw new QueryNotAllowedException("Index name must not be empty");
        }
        String concrete = logicalToConcrete.get(logicalName);
        if (concrete == null) {
            throw new QueryNotAllowedException("Index '" + logicalName + "' is not in the allowlist");
        }
        if (concrete.startsWith(".")) {
            throw new QueryNotAllowedException("System indices ('.'-prefixed) are not readable");
        }
        return concrete;
    }

    /** Whether a concrete index name corresponds to an allowlisted, non-system target. */
    public boolean isAllowedConcrete(String concreteIndex) {
        return concreteIndex != null
                && !concreteIndex.startsWith(".")
                && logicalToConcrete.containsValue(concreteIndex);
    }

    /** The logical name mapped to a concrete index, if any. */
    public Optional<String> logicalFor(String concreteIndex) {
        return logicalToConcrete.entrySet().stream()
                .filter(e -> e.getValue().equals(concreteIndex))
                .map(Map.Entry::getKey)
                .findFirst();
    }
}
