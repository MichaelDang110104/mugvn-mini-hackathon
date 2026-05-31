variable "aws_region" {
  type        = string
  description = "AWS region for the production workload."
}

variable "domain_name" {
  type        = string
  description = "Public domain name to serve (e.g. app.example.com)."
}

variable "api_domain_name" {
  type        = string
  description = "Optional API domain name (e.g. api.example.com)."
  default     = null
}

variable "route53_zone_id" {
  type        = string
  description = "Route53 hosted zone ID that contains domain_name."
}

variable "cloudfront_origin_header_secret" {
  type        = string
  description = "Secret value CloudFront adds as an origin header; ALB rejects if missing/mismatched."
  sensitive   = true
}
