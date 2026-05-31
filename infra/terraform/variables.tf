variable "aws_region" {
  type        = string
  description = "AWS region for the workload (ALB/ECS/VPC/etc)."
}

variable "github_repo" {
  type        = string
  description = "GitHub repo in owner/name form used for OIDC trust."
  default     = null
}

variable "domain_name" {
  type        = string
  description = "Primary production domain name (e.g. app.example.com)."
}

variable "api_domain_name" {
  type        = string
  description = "Optional API domain name (e.g. api.example.com)."
  default     = null
}

variable "route53_zone_id" {
  type        = string
  description = "Route53 hosted zone ID for domain_name."
}

variable "cloudfront_origin_header_secret" {
  type        = string
  description = "Secret CloudFront injects to ALB (origin verification)."
  sensitive   = true
}
