resource "kubernetes_config_map" "billing" {
  metadata {
    name      = "${var.service_name}-config"
    namespace = kubernetes_namespace.billing.metadata[0].name
  }

  data = {
    SPRING_PROFILES_ACTIVE = var.environment
    SERVER_PORT            = tostring(var.app_port)

    # DynamoDB Configuration
    DYNAMODB_TABLE_NAME = aws_dynamodb_table.payments.name

    # AWS Region
    AWS_REGION = var.region

    # AWS DynamoDB Endpoint (empty for production AWS-managed endpoint)
    # For local development, this would be "http://dynamodb-local:8000"
    AWS_DYNAMODB_ENDPOINT = ""

    # Mercado Pago Public Key (non-sensitive)
    MERCADOPAGO_PUBLIC_KEY = var.mercadopago_public_key

    # JVM Options
    JAVA_OPTS = "-Xms512m -Xmx1024m -XX:+UseG1GC"

    # Logging
    LOGGING_LEVEL_ROOT            = "INFO"
    LOGGING_LEVEL_COM_FIAP        = "DEBUG"
    LOGGING_LEVEL_SOFTWARE_AMAZON = "INFO"
  }
}
