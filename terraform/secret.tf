resource "kubernetes_secret" "billing" {
  metadata {
    name      = "${var.service_name}-secret"
    namespace = kubernetes_namespace.billing.metadata[0].name
  }

  data = {
    # AWS Credentials (SDK expects these env var names)
    AWS_ACCESS_KEY_ID     = var.aws_access_key
    AWS_SECRET_ACCESS_KEY = var.aws_secret_key
    # backward-compat (some configs reference these names)
    AWS_ACCESS_KEY = var.aws_access_key
    AWS_SECRET_KEY = var.aws_secret_key
    
    # Mercado Pago
    MERCADOPAGO_ACCESS_TOKEN = var.mercadopago_access_token
  }

  type = "Opaque"
}
