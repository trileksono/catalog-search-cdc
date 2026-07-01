# sample-micro-services

Small playground for learning event-driven architecture with Spring Boot + Kafka + Debezium + Elasticsearch.

The idea is simple: product and category live in their own services, every change is tracked via the outbox pattern, Debezium tails the outbox tables and publishes to Kafka. A search-service consumes those streams, joins product with its category, and writes the enriched document to Elasticsearch so it's searchable.

## Services

| Service | Port | Role |
|---|---|---|
| product-service | 8081 | product CRUD, writes outbox |
| category-service | 8082 | category CRUD, writes outbox |
| search-service | 8083 | Kafka Streams join, sink to ES |

## Infra

- Postgres (product_db, category_db) with `wal_level=logical` for CDC
- Kafka + Zookeeper
- Debezium Connect (postgres connector)
- Elasticsearch 8
- Redis (cache for product/category)

## Running it

```bash
docker compose up --build
```

Wait until kafka-connect is healthy — connectors register themselves via the `connect-setup` one-shot container.

Then try:

```bash
# create a category
curl -X POST http://localhost:8082/api/categories \
  -H "Content-Type: application/json" \
  -d '{"name":"Electronics"}'

# create a product
curl -X POST http://localhost:8081/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Laptop","price":12000000,"categoryId":1,"stock":10,"active":true}'

# give it 2-3 seconds for CDC to flow through Kafka Streams into ES
curl "http://localhost:8083/api/search?q=Laptop"
```

## Data flow

```
[POST product] -> product_db (outbox_events) --Debezium--> Kafka(product-events) --+
                                                                                   |
[KTable category] <- Kafka(category-events) <- Debezium <- category_db (outbox) ---+
                                        |
                                  Kafka Streams join
                                        |
                                        v
                              Elasticsearch (index: products)
                                        |
                                        v
                              GET /api/search
```

## Notes

- The outbox row is written in the same transaction as the main mutation, so there's no race between the DB commit and the event being emitted.
- Connectors use the `ExtractNewRecordState` + `RegexRouter` SMTs so topics stay clean (`product-events`, `category-events`) without the Debezium envelope wrapper.
- Serdes are passed explicitly to every Kafka Streams operator. The default `JsonSerde` can't infer the target type during repartition and the topology falls over.
