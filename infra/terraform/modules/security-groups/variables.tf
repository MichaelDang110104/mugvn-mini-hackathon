variable "name" {
  type        = string
  description = "Prefix/name for resource naming."
}

variable "vpc_id" {
  type        = string
  description = "VPC ID."
}

variable "alb_ingress_port" {
  type        = number
  description = "ALB listener port for CloudFront origin traffic (V1 uses HTTP)."
  default     = 80
}
