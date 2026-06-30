package com.tleksono.search.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tleksono.search.dto.EnrichedProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ElasticsearchProductRepository {

    private final ElasticsearchClient client;
    private final ObjectMapper objectMapper;

    @Value("${app.elasticsearch.index}")
    private String index;

    public void index(EnrichedProductResponse product) throws IOException {
        client.index(i -> i.index(index).id(String.valueOf(product.id())).document(product));
        log.info("Indexed product {} into ES", product.id());
    }

    public void delete(Long id) throws IOException {
        client.delete(d -> d.index(index).id(String.valueOf(id)));
    }

    public List<EnrichedProductResponse> search(String q, int size) throws IOException {
        Query query = Query.of(b -> b.multiMatch(m -> m
                .query(q)
                .fields("name^3", "categoryName")
                .fuzziness("AUTO")));

        SearchResponse<Map> response = client.search(s -> s
                .index(index)
                .query(query)
                .size(size), Map.class);

        return response.hits().hits().stream()
                .map(Hit::source)
                .filter(java.util.Objects::nonNull)
                .map(src -> objectMapper.convertValue(src, EnrichedProductResponse.class))
                .toList();
    }

    public void ensureIndexExists() throws IOException {
        boolean exists = client.indices().exists(e -> e.index(index)).value();
        if (!exists) {
            client.indices().create(c -> c
                    .index(index)
                    .mappings(m -> m
                            .properties("id", p -> p.long_(l -> l))
                            .properties("name", p -> p.text(t -> t
                                    .analyzer("standard")
                                    .fields("keyword", k -> k.keyword(kk -> kk.ignoreAbove(256)))))
                            .properties("price", p -> p.scaledFloat(sf -> sf.scalingFactor(100.0)))
                            .properties("categoryId", p -> p.long_(l -> l))
                            .properties("categoryName", p -> p.text(t -> t
                                    .analyzer("standard")
                                    .fields("keyword", k -> k.keyword(kk -> kk))))
                            .properties("stock", p -> p.integer(i -> i))
                            .properties("active", p -> p.boolean_(b -> b))));
            log.info("Created ES index '{}'", index);
        }
    }
}
