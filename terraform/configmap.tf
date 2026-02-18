resource "kubernetes_config_map" "billing" {
  metadata {
    name      = "${var.service_name}-config"
    namespace = kubernetes_namespace.billing.metadata[0].name
  }

  data = {
    SPRING_PROFILES_ACTIVE = var.environment
    SERVER_PORT            = tostring(var.app_port)
    
    # DynamoDB
    AWS_DYNAMODB_TABLE_NAME = data.terraform_remote_state.dynamodb_billing.outputs.table_name
    
    # AWS Region
    AWS_REGION = var.region
    
    # SQS Queue (se configurado)
    #SQS_QUEUE_URL = var.sqs_queue_url
    
    # Mercado Pago Public Key (não sensível)
    MERCADOPAGO_PUBLIC_KEY = var.mercadopago_public_key
    
    # JVM Options
    JAVA_OPTS = "-Xms512m -Xmx1024m -XX:+UseG1GC"
    
    # Logging
    LOGGING_LEVEL_ROOT = "INFO"
    LOGGING_LEVEL_COM_FIAP = "DEBUG"
  }
}
