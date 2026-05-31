package ai.enterprise.mcp.elastic.adapter;

import ai.enterprise.mcp.elastic.config.ElasticsearchProperties;
import ai.enterprise.mcp.elastic.domain.model.SearchModels.DocumentResult;
import ai.enterprise.mcp.elastic.domain.model.SearchModels.IndexMappings;
import ai.enterprise.mcp.elastic.port.SearchPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHost;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link ElasticsearchSearchAdapter} against a real Elasticsearch via
 * Testcontainers (ADR-0003 §7). Verifies the read endpoints and the 404 → not-found
 * path that unit tests cannot reach. Skipped automatically when Docker is absent.
 */
@Testcontainers(disabledWithoutDocker = true)
class ElasticsearchSearchAdapterIT {

    private static final String IMAGE = "docker.elastic.co/elasticsearch/elasticsearch:8.15.3";
    private static final String INDEX = "products-v1";

    @Container
    static final ElasticsearchContainer ES = new ElasticsearchContainer(DockerImageName.parse(IMAGE))
            .withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node");

    private static RestClient client;
    private static ElasticsearchSearchAdapter adapter;

    @BeforeAll
    static void setUp() throws IOException {
        client = RestClient.builder(HttpHost.create("http://" + ES.getHttpHostAddress())).build();
        index("1", "{\"name\":\"widget\",\"price\":10}");
        index("2", "{\"name\":\"gadget\",\"price\":20}");

        ElasticsearchProperties props = new ElasticsearchProperties();
        props.getLimits().setRequestTimeoutSeconds(5);
        adapter = new ElasticsearchSearchAdapter(client, new ObjectMapper(), props);
    }

    @AfterAll
    static void tearDown() throws IOException {
        if (client != null) {
            client.close();
        }
    }

    private static void index(String id, String json) throws IOException {
        Request req = new Request("PUT", "/" + INDEX + "/_doc/" + id);
        req.addParameter("refresh", "true");
        req.setJsonEntity(json);
        client.performRequest(req);
    }

    @Test
    void listIndicesReportsSeededIndexWithCount() {
        List<SearchPort.ConcreteIndex> indices = adapter.listIndices();
        assertThat(indices)
                .anySatisfy(ci -> {
                    assertThat(ci.name()).isEqualTo(INDEX);
                    assertThat(ci.docCount()).isEqualTo(2);
                });
    }

    @Test
    void describeIndexReturnsMappings() {
        IndexMappings mappings = adapter.describeIndex(INDEX);
        assertThat(mappings.mappings()).isNotEmpty();
        assertThat(mappings.settings()).isNotEmpty();
    }

    @Test
    void searchReturnsBoundedHits() {
        SearchPort.SearchPage page = adapter.search(
                INDEX, Map.of("query", Map.of("match_all", Map.of())), 10, 0, null);
        assertThat(page.totalHits()).isEqualTo(2);
        assertThat(page.hits()).hasSize(2);
        assertThat(page.hits()).allSatisfy(h -> assertThat(h.source()).containsKey("name"));
    }

    @Test
    void searchHonorsSourceProjection() {
        SearchPort.SearchPage page = adapter.search(
                INDEX, Map.of("query", Map.of("match_all", Map.of())), 10, 0, List.of("name"));
        assertThat(page.hits()).allSatisfy(h -> {
            assertThat(h.source()).containsKey("name");
            assertThat(h.source()).doesNotContainKey("price");
        });
    }

    @Test
    void countMatchesSeededDocuments() {
        long count = adapter.count(INDEX, Map.of("query", Map.of("match_all", Map.of())));
        assertThat(count).isEqualTo(2);
    }

    @Test
    void getDocumentReturnsFoundDocument() {
        DocumentResult doc = adapter.getDocument(INDEX, "1", null);
        assertThat(doc.found()).isTrue();
        assertThat(doc.source()).containsEntry("name", "widget");
    }

    @Test
    void getMissingDocumentReturnsNotFound() {
        DocumentResult doc = adapter.getDocument(INDEX, "404", null);
        assertThat(doc.found()).isFalse();
        assertThat(doc.source()).isEmpty();
    }
}
