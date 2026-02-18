resource "kubernetes_secret" "billing" {
  metadata {
    name      = "${var.service_name}-secret"
    namespace = kubernetes_namespace.billing.metadata[0].name
  }

  data = {
    # AWS Credentials for DynamoDB and SQS
    # In production, consider using IRSA (IAM Roles for Service Accounts) instead of storing credentials
    # IRSA is more secure and follows AWS best practices
    AWS_ACCESS_KEY = var.aws_access_key
    AWS_SECRET_KEY = var.aws_secret_key

    # Mercado Pago Access Token (sensitive)
    MERCADOPAGO_ACCESS_TOKEN = var.mercadopago_access_token
  }

  type = "Opaque"
}

# Note: Production Recommendation
# ==============================
# In production, instead of storing AWS credentials in Kubernetes Secrets:
# 1. Enable IRSA (IAM Roles for Service Accounts) in your EKS cluster
# 2. Create an IAM role with DynamoDB and SQS permissions
# 3. Associate the IAM role with the Service Account of this deployment
# 4. Remove AWS_ACCESS_KEY and AWS_SECRET_KEY from this secret
#
# Benefits of IRSA:
# - Temporary, auto-rotating credentials
# - No long-lived keys to compromise
# - Fine-grained IAM permissions
# - Audit trail in CloudTrail
#
# See: https://docs.aws.amazon.com/eks/latest/userguide/iam-roles-for-service-accounts.html

