package ai.enterprise.mcp.elastic.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Externalized configuration for the read-only Elasticsearch MCP server.
 *
 * <pre>
 * mcpes:
 *   connection:
 *     host: https://localhost:9200
 *     api-key: ${ES_API_KEY}
 *   allowlist:               # logical name -> concrete index/alias
 *     customers: customers-v3
 *   limits:
 *     default-page-size: 10
 *     max-page-size: 100
 *     max-result-window: 10000
 *     request-timeout-seconds: 10
 * </pre>
 */
@ConfigurationProperties(prefix = "mcpes")
public class ElasticsearchProperties {

    private Connection connection = new Connection();
    private Map<String, String> allowlist = Map.of();
    private Limits limits = new Limits();

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public Map<String, String> getAllowlist() {
        return allowlist;
    }

    public void setAllowlist(Map<String, String> allowlist) {
        this.allowlist = allowlist;
    }

    public Limits getLimits() {
        return limits;
    }

    public void setLimits(Limits limits) {
        this.limits = limits;
    }

    /** Connection to the Elasticsearch cluster (TLS; least-privilege API key). */
    public static class Connection {
        private String host = "https://localhost:9200";
        private String apiKey = "";

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }

    /** Result-bounding limits (see {@code SearchLimits}). */
    public static class Limits {
        private int defaultPageSize = 10;
        private int maxPageSize = 100;
        private int maxResultWindow = 10_000;
        private int requestTimeoutSeconds = 10;

        public int getDefaultPageSize() {
            return defaultPageSize;
        }

        public void setDefaultPageSize(int defaultPageSize) {
            this.defaultPageSize = defaultPageSize;
        }

        public int getMaxPageSize() {
            return maxPageSize;
        }

        public void setMaxPageSize(int maxPageSize) {
            this.maxPageSize = maxPageSize;
        }

        public int getMaxResultWindow() {
            return maxResultWindow;
        }

        public void setMaxResultWindow(int maxResultWindow) {
            this.maxResultWindow = maxResultWindow;
        }

        public int getRequestTimeoutSeconds() {
            return requestTimeoutSeconds;
        }

        public void setRequestTimeoutSeconds(int requestTimeoutSeconds) {
            this.requestTimeoutSeconds = requestTimeoutSeconds;
        }
    }
}
