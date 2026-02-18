region       = "us-east-2"
environment  = "dev"
project_name = "challengeone"
service_name = "billing"

# Application
app_replicas = 1  # Reduzido para dev
app_image    = "thiagotierre/billing-service:latest"
app_port     = 8080

# Variáveis sensíveis devem ser passadas via -var ou variáveis de ambiente (secret.tfvars):
# - aws_access_key (para DynamoDB e SQS)
# - aws_secret_key (para DynamoDB e SQS)
# - mercadopago_access_token

# Mercado Pago (public key não é sensível)
mercadopago_public_key = "TEST-c5f78030-b1af-4a2d-8ff6-0a3752bfd11e"

# HPA Configuration
hpa_min_replicas  = 1  # Reduzido para dev
hpa_max_replicas  = 3  # Reduzido para dev
hpa_cpu_threshold = 80
