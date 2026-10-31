output "ecr_repository_url" {
  value = aws_ecr_repository.app.repository_url
}

output "artifact_bucket" {
  value = aws_s3_bucket.artifacts.bucket
}

output "ec2_instance_profile_name" {
  value = aws_iam_instance_profile.ec2.name
}

output "pipeline_name" {
  value = aws_codepipeline.app.name
}
