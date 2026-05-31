resource "aws_secretsmanager_secret" "mongo_uri" {
  name = "${var.name}/MONGO_URI"
}

resource "aws_secretsmanager_secret" "openai_api_key" {
  name = "${var.name}/OPENAI_API_KEY"
}

resource "aws_secretsmanager_secret" "redis_host" {
  name = "${var.name}/REDIS_HOST"
}

resource "aws_secretsmanager_secret" "jwt_secret" {
  name = "${var.name}/JWT_SECRET"
  tags = {
    Note = "Hackathon-only: JWT secret is hardcoded in app; do not rely on this in production."
  }
}
