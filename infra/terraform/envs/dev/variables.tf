variable "region" { type = string }
variable "name_prefix" { type = string }
variable "bucket_name" { type = string }
variable "key_prefix" { type = string }
variable "source_system" { type = string }
variable "message_group_id" { type = string }
variable "tags" { type = map(string) default = {} }
variable "force_destroy" {
  type        = bool
  default     = false
}

variable "enable_versioning" {
  type        = bool
  default     = true
}

