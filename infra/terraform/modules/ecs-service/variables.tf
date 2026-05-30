variable "name" {
  type        = string
  description = "Prefix/name for resource naming."
}

variable "service_name" {
  type        = string
  description = "Short service name (frontend/backend)."
}

variable "cluster_arn" {
  type        = string
  description = "ECS cluster ARN."
}

variable "private_subnet_ids" {
  type        = list(string)
  description = "Private subnet IDs for tasks."
}

variable "security_group_id" {
  type        = string
  description = "Security group ID for tasks."
}

variable "target_group_arn" {
  type        = string
  description = "Target group ARN."
}

variable "container_name" {
  type        = string
  description = "Container name in the task definition."
}

variable "container_port" {
  type        = number
  description = "Container port to register to the target group."
}

variable "desired_count" {
  type    = number
  default = 1
}

variable "task_definition_arn" {
  type        = string
  description = "Initial task definition ARN (Terraform bootstrap)."
}
