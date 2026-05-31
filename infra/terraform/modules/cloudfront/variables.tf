variable "name" {
  type        = string
  description = "Prefix/name for resource naming."
}

variable "domain_name" {
  type        = string
  description = "Primary public domain name for the distribution."
}

variable "api_domain_name" {
  type        = string
  description = "Optional API domain alias for the same distribution."
  default     = null
}

variable "alb_dns_name" {
  type        = string
  description = "ALB DNS name used as CloudFront origin."
}

variable "origin_header_name" {
  type        = string
  description = "Origin header name to inject."
  default     = "X-Origin-Verify"
}

variable "origin_header_value" {
  type        = string
  description = "Origin header value to inject."
  sensitive   = true
}

variable "waf_web_acl_arn" {
  type        = string
  description = "WAF Web ACL ARN to attach (CloudFront scope)."
  default     = null
}

variable "hosted_zone_id" {
  type        = string
  description = "Route53 hosted zone ID."
}
