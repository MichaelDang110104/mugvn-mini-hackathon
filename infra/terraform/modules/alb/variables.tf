variable "name" {
  type        = string
  description = "Prefix/name for resource naming."
}

variable "vpc_id" {
  type        = string
  description = "VPC ID."
}

variable "public_subnet_ids" {
  type        = list(string)
  description = "Public subnet IDs for ALB."
}

variable "alb_security_group_id" {
  type        = string
  description = "ALB security group ID."
}

variable "origin_header_name" {
  type        = string
  description = "Header name CloudFront injects."
  default     = "X-Origin-Verify"
}

variable "origin_header_value" {
  type        = string
  description = "Expected header value from CloudFront."
  sensitive   = true
}

variable "app_domain_name" {
  type        = string
  description = "App domain name (Host header)"
  default     = null
}

variable "api_domain_name" {
  type        = string
  description = "API domain name (Host header)"
  default     = null
}
