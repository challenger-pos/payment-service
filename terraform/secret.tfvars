# ===============================================
# SECRET VARIABLES - PAYMENT SERVICE
# ===============================================
#
# Este arquivo contém variáveis sensíveis para o Payment Service.
# NÃO commitar este arquivo no Git!
#
# Uso: terraform apply -var-file=terraform.tfvars -var-file=secret.tfvars
#

# AWS Credentials (para acesso ao DynamoDB e SQS)
aws_access_key = "AKIAWER5733XQTQISL6O"
aws_secret_key = "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"

# Mercado Pago Access Token (produção usa token real)
mercadopago_access_token = "TEST-2418882740517708-021123-3284a32c834a784ab7abe89af125def1-306429912"