#!/bin/sh
set -e

echo "Waiting for Kafka Connect at http://kafka-connect:8083 ..."
until curl -sf http://kafka-connect:8083/connectors >/dev/null 2>&1; do
  sleep 2
done
echo "Kafka Connect is up."

echo "Registering product-outbox-connector..."
curl -sS -X PUT http://kafka-connect:8083/connectors/product-outbox-connector/config \
  -H "Content-Type: application/json" \
  -d @/connectors/product-connector.json
echo ""

echo "Registering category-outbox-connector..."
curl -sS -X PUT http://kafka-connect:8083/connectors/category-outbox-connector/config \
  -H "Content-Type: application/json" \
  -d @/connectors/category-connector.json
echo ""

echo "Connector registration complete."
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --delete --topic search-service-streams-KSTREAM-TOTABLE-0000000004-repartition 2>/dev/null && docker exec kafka kafka-topics --bootstrap-server localhost:9092 --delete --topic search-service-streams-KSTREAM-KEY-SELECT-0000000013-repartition 2>/dev/null && docker exec kafka kafka-topics --bootstrap-server localhost:9092 --delete --topic search-service-streams-KSTREAM-KEY-SELECT-0000000010-repartition 2>/dev/null