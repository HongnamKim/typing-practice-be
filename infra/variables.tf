variable "region" {
  type    = string
  default = "ap-northeast-2"
}

variable "project" {
  type    = string
  default = "typing-practice-be"
}

variable "github_repository" {
  type    = string
  default = "HongnamKim/typing-practice-be"
}

variable "deploy_branch" {
  type    = string
  default = "prod"
}

variable "ssm_prefix" {
  type    = string
  default = "/typing-practice/prod"
}
