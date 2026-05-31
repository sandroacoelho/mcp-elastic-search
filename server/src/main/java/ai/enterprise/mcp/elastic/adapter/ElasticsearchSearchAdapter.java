package ai.enterprise.mcp.elastic.adapter;

import ai.enterprise.mcp.elastic.config.ElasticsearchProperties;
import ai.enterprise.mcp.elastic.domain.model.SearchModels.DocumentResult;
import ai.enterprise.mcp.elastic.domain.model.SearchModels.Hit;
import ai.enterprise.mcp.elastic.domain.model.SearchModels.IndexMappings;
import ai.enterprise.mcp.elastic.port.SearchPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link SearchPort} implementation over the low-level Elasticsearch REST client.
 * Only read endpoints are issued ({@code _cat}, {@code _mapping}, {@code _settings},
 * {@code _search}, {@code _count}, {@code _doc}); no mutating request is ever
 * constructed here, complementing the {@code QueryGuard} and the read-only role
 * (ADR-0003 §2, threat E1/E5). This class is excluded from the unit-coverage gate
 * and exercised by Testcontainers integration tests in CI.
 */
@Component
public class ElasticsearchSearchAdapter implements SearchPort {

    private final RestClient client;
    private final ObjectMapper mapper;
    private final int timeoutSeconds;

    public ElasticsearchSearchAdapter(RestClient client, ObjectMapper mapper, ElasticsearchProperties props) {
        this.client = client;
        this.mapper = mapper;
        this.timeoutSeconds = props.getLimits().getRequestTimeoutSeconds();
    }

    @Override
    public List<ConcreteIndex> listIndices() {
        Request request = new Request("GET", "/_cat/indices");
        request.addParameter("format", "json");
        request.addParameter("h", "index,docs.count");
        List<Map<String, Object>> rows = performForList(request);
        List<ConcreteIndex> out = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            String name = String.valueOf(row.get("index"));
            out.add(new ConcreteIndex(name, parseLong(row.get("docs.count"))));
        }
        return out;
    }

    @Override
    public IndexMappings describeIndex(String concreteIndex) {
        Map<String, Object> mapping = performForMap(new Request("GET", "/" + enc(concreteIndex) + "/_mapping"));
        Map<String, Object> settings = performForMap(new Request("GET", "/" + enc(concreteIndex) + "/_settings"));
        return new IndexMappings(concreteIndex, mapping, settings);
    }

    @Override
    public SearchPage search(String concreteIndex, Map<String, Object> body, int size, int from,
                             List<String> sourceFields) {
        Map<String, Object> req = new LinkedHashMap<>(body == null ? Map.of() : body);
        req.put("size", size);
        req.put("from", from);
        req.put("timeout", timeoutSeconds + "s");
        if (sourceFields != null && !sourceFields.isEmpty()) {
            req.put("_source", sourceFields);
        }
        Request request = new Request("POST", "/" + enc(concreteIndex) + "/_search");
        request.setJsonEntity(toJson(req));
        Map<String, Object> resp = performForMap(request);

        @SuppressWarnings("unchecked")
        Map<String, Object> hitsNode = (Map<String, Object>) resp.getOrDefault("hits", Map.of());
        @SuppressWarnings("unchecked")
        Map<String, Object> total = (Map<String, Object>) hitsNode.getOrDefault("total", Map.of());
        long totalHits = parseLong(total.get("value"));
        boolean lowerBound = "gte".equals(String.valueOf(total.get("relation")));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawHits = (List<Map<String, Object>>) hitsNode.getOrDefault("hits", List.of());
        List<Hit> hits = new ArrayList<>(rawHits.size());
        for (Map<String, Object> h : rawHits) {
            Object score = h.get("_score");
            @SuppressWarnings("unchecked")
            Map<String, Object> src = (Map<String, Object>) h.getOrDefault("_source", Map.of());
            hits.add(new Hit(String.valueOf(h.get("_id")), score instanceof Number n ? n.doubleValue() : 0.0, src));
        }
        return new SearchPage(totalHits, lowerBound, hits);
    }

    @Override
    public long count(String concreteIndex, Map<String, Object> body) {
        Request request = new Request("POST", "/" + enc(concreteIndex) + "/_count");
        if (body != null && body.containsKey("query")) {
            request.setJsonEntity(toJson(Map.of("query", body.get("query"))));
        }
        Map<String, Object> resp = performForMap(request);
        return parseLong(resp.get("count"));
    }

    @Override
    public DocumentResult getDocument(String concreteIndex, String id, List<String> sourceFields) {
        Request request = new Request("GET", "/" + enc(concreteIndex) + "/_doc/" + enc(id));
        if (sourceFields != null && !sourceFields.isEmpty()) {
            request.addParameter("_source_includes", String.join(",", sourceFields));
        }
        try {
            Map<String, Object> resp = performForMap(request);
            boolean found = Boolean.TRUE.equals(resp.get("found"));
            @SuppressWarnings("unchecked")
            Map<String, Object> src = (Map<String, Object>) resp.getOrDefault("_source", Map.of());
            return new DocumentResult(concreteIndex, id, found, src);
        } catch (UncheckedIOException e) {
            // A missing document surfaces as a 404 ResponseException (an IOException),
            // wrapped by performForMap. Treat it as "not found" rather than an error.
            if (e.getCause() instanceof ResponseException re
                    && re.getResponse().getStatusLine().getStatusCode() == 404) {
                return new DocumentResult(concreteIndex, id, false, Map.of());
            }
            throw e;
        }
    }

    // --- helpers ---

    private Map<String, Object> performForMap(Request request) {
        try (InputStream in = perform(request)) {
            return mapper.readValue(in, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<Map<String, Object>> performForList(Request request) {
        try (InputStream in = perform(request)) {
            return mapper.readValue(in,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {
                    });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private InputStream perform(Request request) throws IOException {
        Response response = client.performRequest(request);
        return response.getEntity().getContent();
    }

    private String toJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String enc(String pathSegment) {
        return URLEncoder.encode(pathSegment, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static long parseLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
