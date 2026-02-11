variable "name_prefix" { type = string }
variable "region" { type = string }
variable "bucket_name" { type = string }
variable "key_prefix" { type = string }
variable "source_system" { type = string }
variable "message_group_id" { type = string }

variable "visibility_timeout_seconds" { type = number default = 900 }
variable "max_receive_count" { type = number default = 5 }
variable "tags" { type = map(string) default = {} }
