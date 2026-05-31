package ai.enterprise.mcp.elastic.domain;

/**
 * Thrown when a request is rejected by the read-only guard rails — a disallowed
 * query shape (scripting, etc.), an index outside the allowlist, or pagination
 * beyond the bounded window. The server fails closed on any of these
 * (ADR-0003 §2; threats E1–E3).
 */
public class QueryNotAllowedException extends RuntimeException {

    public QueryNotAllowedException(String message) {
        super(message);
    }
}
