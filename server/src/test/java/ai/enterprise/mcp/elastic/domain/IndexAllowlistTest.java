package ai.enterprise.mcp.elastic.domain;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IndexAllowlistTest {

    private IndexAllowlist allowlist() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("customers", "customers-v3");
        map.put("secret", ".internal-index");
        return new IndexAllowlist(map);
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
}
