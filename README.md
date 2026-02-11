# S3 → EventBridge → SQS FIFO → Spring WebFlux Ingestion → MongoDB (Production-ready starter)

This repo provides:
- **Infrastructure as Code** (Terraform) for **dev/qa/prod**
- **Spring Boot (Kotlin) + WebFlux** ingestion service
- **Adapter / Ports & Adapters** architecture
- **Idempotency** (safe retries) + **bulk upsert** into MongoDB
- **SQS FIFO** consumption with long polling + delete-on-success
- **EventBridge Input Transformer** to create a stable envelope JSON

## Architecture

S3 (ObjectCreated) → EventBridge Rule → SQS FIFO (+ DLQ) → Ingestion Service → MongoDB

SQS body (envelope) is created by EventBridge and looks like:

```json
{
  "schemaVersion": "1.0",
  "eventType": "Object Created",
  "sourceSystem": "order-calc-services",
  "producedAt": "2026-02-10T15:03:17Z",
  "region": "us-east-2",
  "file": { "bucket": "my-bucket", "key": "incoming/order_calc_services_OCS.20260130150317" },
  "trace": { "correlationId": "uuid", "account": "123456789012" }
}
```

The ingestion service then:
1) Parses the envelope (bucket/key)
2) Calls S3 HeadObject (ETag/version/size) for idempotency + auditing
3) Streams the S3 object line-by-line (no full file in memory)
4) Parses pipe-delimited rows into domain records
5) Bulk upserts into Mongo
6) Marks job DONE and deletes SQS message

## Local development

### Run Mongo locally
```bash
docker compose up -d mongo
```

### Run the service
```bash
cd app
./gradlew bootRun
```

### Environment variables / config
See `app/src/main/resources/application.yml`.

## Terraform

Terraform is under `infra/terraform`:
- `modules/ingestion_pipeline` reusable module
- `envs/dev`, `envs/qa`, `envs/prod` wrappers

Typical usage:
```bash
cd infra/terraform/envs/dev
terraform init
terraform plan
terraform apply
```

## Notes / Best practices included

- SQS FIFO + DLQ redrive
- EventBridge permissioned to send to SQS via queue policy
- Idempotency (Mongo `ingestion_jobs` unique index on bucket+key+eTag)
- Bulk upsert for high performance
- Structured logging + correlationId
- Actuator health endpoints

