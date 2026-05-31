output "mongo_uri_secret_arn" {
  value = aws_secretsmanager_secret.mongo_uri.arn
}

output "openai_api_key_secret_arn" {
  value = aws_secretsmanager_secret.openai_api_key.arn
}

output "redis_host_secret_arn" {
  value = aws_secretsmanager_secret.redis_host.arn
}

output "jwt_secret_secret_arn" {
  value = aws_secretsmanager_secret.jwt_secret.arn
}
