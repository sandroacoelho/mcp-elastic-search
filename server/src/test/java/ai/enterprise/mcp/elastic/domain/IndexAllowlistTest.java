package ai.enterprise.mcp.elastic.domain;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IndexAllowlistTest {

    private IndexAllowlist allowlist() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("customers", "customers-v3");
        map.put("secret", ".internal-index");
        return new IndexAllowlist(map, Map.of("customers", List.of("id", "name")));
    }

    @Test
    void nullMapYieldsEmptyAllowlist() {
        assertThat(new IndexAllowlist(null).logicalNames()).isEmpty();
    }

    @Test
    void exposesLogicalNames() {
        assertThat(allowlist().logicalNames()).containsExactlyInAnyOrder("customers", "secret");
    }

    @Test
    void resolvesKnownLogicalName() {
        assertThat(allowlist().resolve("customers")).isEqualTo("customers-v3");
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> allowlist().resolve(" "))
                .isInstanceOf(QueryNotAllowedException.class)
                .hasMessageContaining("must not be empty");
    }

    @Test
    void rejectsNullName() {
        assertThatThrownBy(() -> allowlist().resolve(null))
                .isInstanceOf(QueryNotAllowedException.class)
                .hasMessageContaining("must not be empty");
    }

    @Test
    void rejectsUnknownName() {
        assertThatThrownBy(() -> allowlist().resolve("orders"))
                .isInstanceOf(QueryNotAllowedException.class)
                .hasMessageContaining("not in the allowlist");
    }

    @Test
    void rejectsSystemIndexTarget() {
        assertThatThrownBy(() -> allowlist().resolve("secret"))
                .isInstanceOf(QueryNotAllowedException.class)
                .hasMessageContaining("System indices");
    }

    @Test
    void isAllowedConcreteChecks() {
        IndexAllowlist a = allowlist();
        assertThat(a.isAllowedConcrete("customers-v3")).isTrue();
        assertThat(a.isAllowedConcrete(".internal-index")).isFalse();
        assertThat(a.isAllowedConcrete("unlisted")).isFalse();
        assertThat(a.isAllowedConcrete(null)).isFalse();
    }

    @Test
    void logicalForConcrete() {
        IndexAllowlist a = allowlist();
        assertThat(a.logicalFor("customers-v3")).contains("customers");
        assertThat(a.logicalFor("nope")).isEmpty();
    }

    @Test
    void sourceFieldsDefaultToConfiguredAllowlist() {
        assertThat(allowlist().sourceFieldsFor("customers", null)).containsExactly("id", "name");
        assertThat(allowlist().sourceFieldsFor("customers", List.of())).containsExactly("id", "name");
    }

    @Test
    void sourceFieldsAllowConfiguredSubset() {
        assertThat(allowlist().sourceFieldsFor("customers", List.of("name"))).containsExactly("name");
    }

    @Test
    void sourceFieldsRejectUnlistedField() {
        assertThatThrownBy(() -> allowlist().sourceFieldsFor("customers", List.of("ssn")))
                .isInstanceOf(QueryNotAllowedException.class)
                .hasMessageContaining("Source field");
    }

    @Test
    void sourceFieldsForUnconfiguredIndexReturnsEmptyProjection() {
        assertThat(new IndexAllowlist(Map.of("logs", "logs-v1")).sourceFieldsFor("logs", null)).isEmpty();
    }

    @Test
    void nullSourceFieldMapReturnsEmptyProjection() {
        assertThat(new IndexAllowlist(Map.of("logs", "logs-v1"), null).sourceFieldsFor("logs", null)).isEmpty();
    }
}
