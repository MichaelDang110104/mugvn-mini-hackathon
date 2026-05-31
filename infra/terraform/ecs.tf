data "aws_iam_policy_document" "ecs_task_assume" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "ecs_task_execution" {
  name               = "${local.project}-ecs-task-execution"
  assume_role_policy = data.aws_iam_policy_document.ecs_task_assume.json
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution" {
  role       = aws_iam_role.ecs_task_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

data "aws_iam_policy_document" "ecs_task_execution_secrets" {
  statement {
    effect = "Allow"
    actions = [
      "secretsmanager:GetSecretValue",
      "secretsmanager:DescribeSecret",
    ]
    resources = [
      module.secrets.mongo_uri_secret_arn,
      module.secrets.openai_api_key_secret_arn,
      module.secrets.redis_host_secret_arn,
      module.secrets.jwt_secret_secret_arn,
    ]
  }
}

resource "aws_iam_role_policy" "ecs_task_execution_secrets" {
  role   = aws_iam_role.ecs_task_execution.id
  policy = data.aws_iam_policy_document.ecs_task_execution_secrets.json
}

resource "aws_iam_role" "ecs_task" {
  name               = "${local.project}-ecs-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_task_assume.json
}

resource "aws_ecs_task_definition" "frontend_bootstrap" {
  family                   = "${local.project}-frontend"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 256
  memory                   = 512
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([
    {
      name      = "frontend"
      image     = "public.ecr.aws/nginx/nginx:stable-alpine"
      essential = true
      portMappings = [
        {
          containerPort = 3000
          hostPort      = 3000
          protocol      = "tcp"
        }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = "/ecs/${local.project}/frontend"
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])
}

resource "aws_ecs_task_definition" "backend_bootstrap" {
  family                   = "${local.project}-backend"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 256
  memory                   = 512
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([
    {
      name      = "backend"
      image     = "public.ecr.aws/docker/library/nginx:stable-alpine"
      essential = true
      portMappings = [
        {
          containerPort = 8686
          hostPort      = 8686
          protocol      = "tcp"
        }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = "/ecs/${local.project}/backend"
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])
}

module "ecs_frontend" {
  source = "./modules/ecs-service"

  name                = local.project
  service_name        = "frontend"
  cluster_arn         = module.ecs_cluster.cluster_arn
  private_subnet_ids  = module.vpc.private_subnet_ids
  security_group_id   = module.security_groups.frontend_sg_id
  target_group_arn    = module.alb.frontend_target_group_arn
  container_name      = "frontend"
  container_port      = 3000
  task_definition_arn = aws_ecs_task_definition.frontend_bootstrap.arn
}

module "ecs_backend" {
  source = "./modules/ecs-service"

  name                = local.project
  service_name        = "backend"
  cluster_arn         = module.ecs_cluster.cluster_arn
  private_subnet_ids  = module.vpc.private_subnet_ids
  security_group_id   = module.security_groups.backend_sg_id
  target_group_arn    = module.alb.backend_target_group_arn
  container_name      = "backend"
  container_port      = 8686
  task_definition_arn = aws_ecs_task_definition.backend_bootstrap.arn
}
