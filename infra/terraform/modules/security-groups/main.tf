data "aws_ec2_managed_prefix_list" "cloudfront_origin" {
  name = "com.amazonaws.global.cloudfront.origin-facing"
}

resource "aws_security_group" "alb" {
  name        = "${var.name}-alb-sg"
  description = "ALB security group"
  vpc_id      = var.vpc_id

  ingress {
    description     = "CloudFront origin-facing"
    from_port       = var.alb_ingress_port
    to_port         = var.alb_ingress_port
    protocol        = "tcp"
    prefix_list_ids = [data.aws_ec2_managed_prefix_list.cloudfront_origin.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.name}-alb-sg"
  }
}

resource "aws_security_group" "frontend" {
  name        = "${var.name}-frontend-sg"
  description = "Frontend ECS tasks"
  vpc_id      = var.vpc_id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.name}-frontend-sg"
  }
}

resource "aws_security_group" "backend" {
  name        = "${var.name}-backend-sg"
  description = "Backend ECS tasks"
  vpc_id      = var.vpc_id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.name}-backend-sg"
  }
}

resource "aws_security_group_rule" "alb_to_frontend" {
  type                     = "ingress"
  security_group_id        = aws_security_group.frontend.id
  from_port                = 3000
  to_port                  = 3000
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.alb.id
  description              = "ALB to frontend"
}

resource "aws_security_group_rule" "alb_to_backend" {
  type                     = "ingress"
  security_group_id        = aws_security_group.backend.id
  from_port                = 8686
  to_port                  = 8686
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.alb.id
  description              = "ALB to backend"
}
