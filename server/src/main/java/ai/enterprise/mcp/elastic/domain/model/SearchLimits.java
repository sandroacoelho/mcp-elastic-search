package ai.enterprise.mcp.elastic.domain.model;

/**
 * Result-bounding limits enforced on every read (ADR-0003 §2, threat E3). Held as
 * a plain value so the service layer stays decoupled from Spring configuration.
 *
 * @param defaultPageSize       page size used when the caller omits one
 * @param maxPageSize           hard ceiling on returned hits per page
 * @param maxResultWindow       ceiling on {@code from + size} (deep-pagination guard)
 * @param requestTimeoutSeconds per-request timeout applied at the backend
 */
public record SearchLimits(int defaultPageSize, int maxPageSize, int maxResultWindow, int requestTimeoutSeconds) {

    public SearchLimits {
        if (defaultPageSize <= 0) {
            defaultPageSize = 10;
        }
        if (maxPageSize <= 0) {
            maxPageSize = 100;
        }
        if (maxResultWindow <= 0) {
            maxResultWindow = 10_000;
        }
        if (requestTimeoutSeconds <= 0) {
            requestTimeoutSeconds = 10;
        }
        if (defaultPageSize > maxPageSize) {
            defaultPageSize = maxPageSize;
        }
    }
}
