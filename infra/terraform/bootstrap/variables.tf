variable "aws_region" {
  type        = string
  description = "AWS region to create the state resources in."
}

variable "state_bucket_name" {
  type        = string
  description = "S3 bucket name for Terraform state."
}

variable "lock_table_name" {
  type        = string
  description = "DynamoDB table name for Terraform state locking."
}
