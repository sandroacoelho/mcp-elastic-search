package ai.enterprise.mcp.elastic.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QueryGuardTest {

    private final QueryGuard guard = new QueryGuard();

    @Test
    void acceptsNullAndEmptyBody() {
        assertThatCode(() -> guard.assertReadOnlySearchBody(null)).doesNotThrowAnyException();
        assertThatCode(() -> guard.assertReadOnlySearchBody(Map.of())).doesNotThrowAnyException();
    }

    @Test
    void acceptsAllowedTopLevelKeysAndScalars() {
        Map<String, Object> body = Map.of(
                "query", Map.of("match_all", Map.of()),
                "sort", List.of("name"),
                "_source", List.of("name", "id"),
                "track_total_hits", true,
                "min_score", 1.0);
        assertThatCode(() -> guard.assertReadOnlySearchBody(body)).doesNotThrowAnyException();
    }

    @Test
    void rejectsUnknownTopLevelKey() {
        assertThatThrownBy(() -> guard.assertReadOnlySearchBody(Map.of("aggs", Map.of())))
                .isInstanceOf(QueryNotAllowedException.class)
                .hasMessageContaining("Unsupported search key");
    }

    @Test
    void rejectsScriptingNestedInMap() {
        Map<String, Object> body = Map.of("query",
                Map.of("function_score", Map.of("script_score", Map.of("script", "doc['x']"))));
        assertThatThrownBy(() -> guard.assertReadOnlySearchBody(body))
                .isInstanceOf(QueryNotAllowedException.class)
                .hasMessageContaining("Scripting construct");
    }

    @Test
    void rejectsScriptingNestedInList() {
        Map<String, Object> body = Map.of("sort", List.of(Map.of("script", Map.of("lang", "painless"))));
        assertThatThrownBy(() -> guard.assertReadOnlySearchBody(body))
                .isInstanceOf(QueryNotAllowedException.class)
                .hasMessageContaining("script");
    }

    @Test
    void rejectsRuntimeMappingsAsScripting() {
        // runtime_mappings is both a disallowed top-level key and a banned scripting construct;
        // nest it under an allowed key to prove the recursive scan catches it.
        Map<String, Object> body = Map.of("query", Map.of("runtime_mappings", Map.of()));
        assertThatThrownBy(() -> guard.assertReadOnlySearchBody(body))
                .isInstanceOf(QueryNotAllowedException.class);
    }
}
