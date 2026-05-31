terraform {
  backend "s3" {}
}

module "core" {
  source = "../../"

  aws_region                      = var.aws_region
  domain_name                     = var.domain_name
  api_domain_name                 = var.api_domain_name
  route53_zone_id                 = var.route53_zone_id
  cloudfront_origin_header_secret = var.cloudfront_origin_header_secret
}
