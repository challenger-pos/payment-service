region       = "us-east-2"
environment  = "homologation"
project_name = "challengeone"
service_name = "billing"

# Application
app_replicas = 2
app_image    = "thiagotierre/billing-service:latest"
app_port     = 8080

# Variáveis sensíveis devem ser passadas via -var ou variáveis de ambiente:
# - db_password
# - aws_access_key
# - aws_secret_key
# - mercadopago_access_token

# Mercado Pago (public key não é sensível)
mercadopago_public_key = "APP_USR-test-public-key"

# SQS Queue (opcional - se usar mensageria)
sqs_queue_url = ""

# HPA Configuration
hpa_min_replicas  = 2
hpa_max_replicas  = 10
hpa_cpu_threshold = 70
