resource "kubernetes_secret" "billing" {
  metadata {
    name      = "${var.service_name}-secret"
    namespace = kubernetes_namespace.billing.metadata[0].name
  }

  data = {
    # Database
    SPRING_DATASOURCE_PASSWORD = var.db_password
    
    # AWS Credentials
    AWS_ACCESS_KEY = var.aws_access_key
    AWS_SECRET_KEY = var.aws_secret_key
    
    # Mercado Pago
    MERCADOPAGO_ACCESS_TOKEN = var.mercadopago_access_token
  }

  type = "Opaque"
}
