module "vpc" {
  source = "./modules/vpc"

  name       = local.project
  aws_region = var.aws_region
}

module "security_groups" {
  source = "./modules/security-groups"

  name   = local.project
  vpc_id = module.vpc.vpc_id
}

module "ecr" {
  source = "./modules/ecr"

  name = local.project
}

module "ecs_cluster" {
  source = "./modules/ecs-cluster"

  name = local.project
}

module "secrets" {
  source = "./modules/secrets-manager"

  name = local.project
}

module "waf" {
  source = "./modules/waf"

  name = local.project
}

module "alb" {
  source = "./modules/alb"

  name                  = local.project
  vpc_id                = module.vpc.vpc_id
  public_subnet_ids     = module.vpc.public_subnet_ids
  alb_security_group_id = module.security_groups.alb_sg_id

  origin_header_value = var.cloudfront_origin_header_secret

  app_domain_name = var.domain_name
  api_domain_name = var.api_domain_name
}

module "cloudfront" {
  source = "./modules/cloudfront"

  providers = {
    aws           = aws
    aws.us_east_1 = aws.us_east_1
  }

  name                = local.project
  domain_name         = var.domain_name
  api_domain_name     = var.api_domain_name
  alb_dns_name        = module.alb.alb_dns_name
  origin_header_value = var.cloudfront_origin_header_secret
  waf_web_acl_arn     = module.waf.web_acl_arn
  hosted_zone_id      = var.route53_zone_id
}

module "github_oidc" {
  count  = var.github_repo == null ? 0 : 1
  source = "./modules/iam-github-oidc"

  name        = local.project
  github_repo = var.github_repo
}
