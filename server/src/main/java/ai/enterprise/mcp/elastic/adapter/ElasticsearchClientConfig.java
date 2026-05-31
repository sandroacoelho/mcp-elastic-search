package ai.enterprise.mcp.elastic.adapter;

import ai.enterprise.mcp.elastic.config.ElasticsearchProperties;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Builds the low-level Elasticsearch REST client. Kept inside the {@code adapter}
 * package so all Elasticsearch-client types stay confined here (ArchUnit rule A2).
 * Authenticates with a least-privilege API key over TLS (ADR-0003 §2, threat E5).
 */
@Configuration
public class ElasticsearchClientConfig {

    @Bean(destroyMethod = "close")
    public RestClient elasticsearchRestClient(ElasticsearchProperties props) {
        HttpHost host = HttpHost.create(props.getConnection().getHost());
        RestClientBuilder builder = RestClient.builder(host);
        String apiKey = props.getConnection().getApiKey();
        if (StringUtils.hasText(apiKey)) {
            builder.setDefaultHeaders(new Header[]{
                    new BasicHeader("Authorization", "ApiKey " + apiKey)
            });
        }
        return builder.build();
    }
}
