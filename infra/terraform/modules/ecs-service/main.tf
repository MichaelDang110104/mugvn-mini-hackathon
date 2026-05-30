resource "aws_cloudwatch_log_group" "this" {
  name              = "/ecs/${var.name}/${var.service_name}"
  retention_in_days = 7
}

resource "aws_ecs_service" "this" {
  name            = "${var.name}-${var.service_name}"
  cluster         = var.cluster_arn
  task_definition = var.task_definition_arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [var.security_group_id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = var.target_group_arn
    container_name   = var.container_name
    container_port   = var.container_port
  }

  lifecycle {
    ignore_changes = [
      task_definition,
    ]
  }
}
