output "vpc_id" {
  value = module.vpc.vpc_id
}

output "aws_region" {
  value = var.aws_region
}

output "public_subnet_ids" {
  value = module.vpc.public_subnet_ids
}

output "private_subnet_ids" {
  value = module.vpc.private_subnet_ids
}

output "nat_eip" {
  value = module.vpc.nat_eip
}

output "alb_sg_id" {
  value = module.security_groups.alb_sg_id
}

output "frontend_sg_id" {
  value = module.security_groups.frontend_sg_id
}

output "backend_sg_id" {
  value = module.security_groups.backend_sg_id
}

output "backend_repository_url" {
  value = module.ecr.backend_repository_url
}

output "frontend_repository_url" {
  value = module.ecr.frontend_repository_url
}

output "ecs_cluster_name" {
  value = module.ecs_cluster.cluster_name
}

output "ecs_cluster_arn" {
  value = module.ecs_cluster.cluster_arn
}

output "frontend_service_name" {
  value = module.ecs_frontend.service_name
}

output "backend_service_name" {
  value = module.ecs_backend.service_name
}

output "ecs_task_execution_role_arn" {
  value = aws_iam_role.ecs_task_execution.arn
}

output "ecs_task_role_arn" {
  value = aws_iam_role.ecs_task.arn
}

output "frontend_log_group" {
  value = module.ecs_frontend.log_group_name
}

output "backend_log_group" {
  value = module.ecs_backend.log_group_name
}

output "alb_dns_name" {
  value = module.alb.alb_dns_name
}

output "cloudfront_domain_name" {
  value = module.cloudfront.distribution_domain_name
}

output "mongo_uri_secret_arn" {
  value = module.secrets.mongo_uri_secret_arn
}

output "openai_api_key_secret_arn" {
  value = module.secrets.openai_api_key_secret_arn
}

output "redis_host_secret_arn" {
  value = module.secrets.redis_host_secret_arn
}

output "jwt_secret_secret_arn" {
  value = module.secrets.jwt_secret_secret_arn
}
