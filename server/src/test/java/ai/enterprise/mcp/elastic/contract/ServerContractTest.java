package ai.enterprise.mcp.elastic.contract;

import ai.enterprise.mcp.elastic.domain.IndexAllowlist;
import ai.enterprise.mcp.elastic.domain.QueryGuard;
import ai.enterprise.mcp.elastic.domain.QueryNotAllowedException;
import ai.enterprise.mcp.elastic.domain.model.SearchLimits;
import ai.enterprise.mcp.elastic.service.SearchService;
import ai.enterprise.mcp.elastic.support.FakeSearchPort;
import ai.enterprise.mcp.elastic.tool.SearchTools;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Server safety-contract conformance for the Elasticsearch read-only server,
 * implementing the language-neutral spec in {@code tests/server-contract/README.md}
 * (ADR-0001 §4 / QA-10). Cases map 1:1 to C1–C6.
 *
 * <p><b>Scope.</b> In this architecture primary security controls live in the gateway
 * (ADR-0001), so the cases the <em>server</em> genuinely owns — query/result shaping
 * (C2), resource scoping (C4), logging hygiene (C5), and fail-closed behaviour (C6) —
 * are asserted here at the enforcement layer. Cases owned by the gateway/transport
 * (C1 token binding) or not applicable to a read-only server (C3 idempotency) are kept
 * and explicitly annotated rather than deleted, per the repo's "mark N/A, don't delete"
 * convention. Full black-box HTTP conformance is a follow-up (ADR-0003 §7).
 */
class ServerContractTest {

    private static SearchService service() {
        return service(new FakeSearchPort());
    }

    private static SearchService service(FakeSearchPort port) {
        IndexAllowlist allowlist = new IndexAllowlist(Map.of("products", "products-v1"));
        return new SearchService(port, new QueryGuard(), allowlist, new SearchLimits(10, 100, 10_000, 10));
    }

    @Nested
    @DisplayName("C1 — accept only gateway-issued, audience-bound downstream tokens")
    class C1 {
        @Test
        @Disabled("Gateway/transport-enforced for this deployment: the server is reachable only "
                + "from the gateway by network policy (ADR-0001 §4; ADR-0003 threats E8/E9). The "
                + "read-only server has no local token layer to assert here.")
        void tokenAudienceBinding_isGatewayEnforced() {
            // intentionally not implemented at the server layer
        }
    }

    @Nested
    @DisplayName("C2 — enforce gateway obligations that shape the query/result")
    class C2 {
        @Test
        @DisplayName("C2.1 — a query that would exceed the cap is bounded to the limit")
        void capsResultSize() {
            FakeSearchPort port = new FakeSearchPort();
            service(port).search("products", null, 10_000, 0, null);
            assertThat(port.lastSize).isEqualTo(100); // maxPageSize
        }

        @Test
        @DisplayName("C2.3 — field-projection obligation is applied")
        void appliesSourceProjection() {
            FakeSearchPort port = new FakeSearchPort();
            service(port).search("products", null, 10, 0, List.of("name"));
            assertThat(port.lastSourceFields).containsExactly("name");
        }

        @Test
        @DisplayName("C2.4 — the server cannot be driven past its bound (deep pagination fails closed)")
        void refusesToExceedResultWindow() {
            assertThatThrownBy(() -> service().search("products", null, 100, 9_999, null))
                    .isInstanceOf(QueryNotAllowedException.class);
        }
    }

    @Nested
    @DisplayName("C3 — preserve idempotency for mutating actions")
    class C3 {
        @Test
        @DisplayName("C3 — N/A: the tool surface is read-only, so every operation is naturally idempotent")
        void noMutatingToolsExist() {
            Set<String> mutatingVerbs = Set.of("create", "update", "delete", "write", "bulk", "reindex", "upsert");
            List<String> toolMethods = Arrays.stream(SearchTools.class.getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(Tool.class))
                    .map(Method::getName)
                    .collect(Collectors.toList());

            assertThat(toolMethods)
                    .containsExactlyInAnyOrder("listIndices", "describeIndex", "search", "getDocument", "count");
            for (String name : toolMethods) {
                assertThat(mutatingVerbs)
                        .as("tool '%s' must not be a mutating operation", name)
                        .noneMatch(verb -> name.toLowerCase().contains(verb));
            }
        }
    }

    @Nested
    @DisplayName("C4 — constrain access to the requested resource and purpose")
    class C4 {
        @Test
        @DisplayName("C4.1 — a scoped request reaches only the resolved concrete index")
        void scopesToResolvedIndex() {
            FakeSearchPort port = new FakeSearchPort();
            service(port).search("products", null, 10, 0, null);
            assertThat(port.lastConcreteIndex).isEqualTo("products-v1");
        }

        @Test
        @DisplayName("C4.2 — widening to an index outside the allowlist fails closed")
        void rejectsUnlistedIndex() {
            assertThatThrownBy(() -> service().search("secret_index", null, 10, 0, null))
                    .isInstanceOf(QueryNotAllowedException.class);
        }

        @Test
        @DisplayName("C4.2 — system ('.'-prefixed) indices are not reachable")
        void rejectsSystemIndex() {
            SearchService svc = new SearchService(new FakeSearchPort(), new QueryGuard(),
                    new IndexAllowlist(Map.of("internal", ".kibana")), new SearchLimits(10, 100, 10_000, 10));
            assertThatThrownBy(() -> svc.search("internal", null, 10, 0, null))
                    .isInstanceOf(QueryNotAllowedException.class);
        }

        @Test
        @DisplayName("C4.3 — query shape cannot be widened into scripting/code execution")
        void rejectsScriptingQueryShape() {
            Map<String, Object> body = Map.of("query", Map.of("script_score", Map.of("script", "x")));
            assertThatThrownBy(() -> service().search("products", body, 10, 0, null))
                    .isInstanceOf(QueryNotAllowedException.class);
        }
    }

    @Nested
    @DisplayName("C5 — emit structured audit metadata without raw regulated payloads")
    class C5 {
        private final JavaClasses production = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("ai.enterprise.mcp.elastic");

        @Test
        @DisplayName("C5.2 — production code cannot leak payloads via java.util.logging or stdout/stderr")
        void noRawPayloadLoggingChannels() {
            // Audit events themselves are emitted by the gateway (ADR-0001 §10); the server's
            // contractual duty is to never write raw payloads to uncontrolled sinks.
            NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING.check(production);
            NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.check(production);
        }
    }

    @Nested
    @DisplayName("C6 — fail closed on missing classification or policy context")
    class C6 {
        @Test
        @DisplayName("C6 — an unknown index name fails closed")
        void blankIndexFailsClosed() {
            assertThatThrownBy(() -> service().describeIndex(" "))
                    .isInstanceOf(QueryNotAllowedException.class);
        }

        @Test
        @DisplayName("C6 — a blank document id fails closed")
        void blankDocumentIdFailsClosed() {
            assertThatThrownBy(() -> service().getDocument("products", "  ", null))
                    .isInstanceOf(QueryNotAllowedException.class);
        }

        @Test
        @DisplayName("C6 — listIndices never surfaces a non-allowlisted index, even if the backend returns it")
        void listIndicesNeverLeaksNonAllowlisted() {
            FakeSearchPort port = new FakeSearchPort();
            port.indices = List.of(
                    new ai.enterprise.mcp.elastic.port.SearchPort.ConcreteIndex("products-v1", 3),
                    new ai.enterprise.mcp.elastic.port.SearchPort.ConcreteIndex(".security-7", 9),
                    new ai.enterprise.mcp.elastic.port.SearchPort.ConcreteIndex("unlisted", 1));
            assertThat(service(port).listIndices())
                    .extracting(s -> s.concreteIndex())
                    .containsExactly("products-v1");
        }
    }
}
