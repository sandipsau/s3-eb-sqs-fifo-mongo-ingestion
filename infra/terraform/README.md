# Terraform

Structure:
- modules/ingestion_pipeline : reusable module
- envs/dev|qa|prod : environment wrappers

Each env uses:
- remote state (configure backend.tf accordingly)
- different names/tags/vars (bucket, prefix, queue names)

