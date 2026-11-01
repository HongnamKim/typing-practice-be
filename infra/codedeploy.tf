locals {
  codedeploy_config = "CodeDeployDefault.OneAtATime"
}

resource "aws_codedeploy_app" "app" {
  name             = var.project
  compute_platform = "Server"
}

resource "aws_codedeploy_deployment_group" "app" {
  app_name               = aws_codedeploy_app.app.name
  deployment_group_name  = var.project
  service_role_arn       = aws_iam_role.codedeploy.arn
  deployment_config_name = local.codedeploy_config

  deployment_style {
    deployment_type   = "IN_PLACE"
    deployment_option = "WITHOUT_TRAFFIC_CONTROL"
  }

  # 태그는 운영 EC2 에 콘솔에서 1회 직접 단다.
  ec2_tag_set {
    ec2_tag_filter {
      key   = "Deployment"
      type  = "KEY_AND_VALUE"
      value = var.project
    }
  }

  # deploy.sh 가 실패 시 스스로 복구하므로 자동 재배포를 겹치지 않는다.
  auto_rollback_configuration {
    enabled = false
  }
}
