# Payment Service - Terraform Configuration

## Overview

Configuração simplificada do Terraform para o Payment Service (Kubernetes Deployment + HPA). Todos os valores não-sensíveis estão como defaults no código, reduzindo dependências de variáveis de ambiente para CI/CD.

## Ambientes

| Propriedade | Dev | Homologation | Production |
|-------------|-----|--------------|-----------|
| `environment` | dev | homologation | production |
| `app_replicas` | 2 | 2 | 2 |
| `hpa_min_replicas` | 2 | 2 | 2 |
| `hpa_max_replicas` | 4 | 4 | 4 |
| `hpa_cpu_threshold` | 70% | 70% | 70% |

Todos os demais valores usam defaults.

## Estrutura de Arquivos

```
terraform/
├── backend.tf                      # S3 backend (sem hardcoded key)
├── deployment.tf                   # Kubernetes deployment
├── secret.tf                       # Kubernetes secret para credenciais
├── configmap.tf                    # Kubernetes configmap
├── service.tf                      # Kubernetes service
├── hpa.tf                          # Horizontal Pod Autoscaler
├── namespace.tf                    # Kubernetes namespace
├── providers.tf                    # Provider configuration
├── variables.tf                    # Variáveis com defaults
├── outputs.tf                      # Outputs
├── terraform.tfvars.dev            # Dev environment (environment only)
├── terraform.tfvars.homologation   # Homolog environment (environment only)
├── terraform.tfvars.production     # Production environment (environment only)
├── secret.tfvars                   # SECRETS (não fazer commit!)
├── secret.tfvars.template          # Exemplo de variáveis secretas
└── terraform.tfvars.example        # Exemplo completo (legado)
```

## Deploy

### Pré-requisitos

- Terraform v1.0+
- AWS credentials configurados
- kubeconfig acessível (EKS cluster)
- Arquivo `secret.tfvars` com credenciais

### Exemplo: Deploy em Dev

```bash
# Initiate with backend config
terraform init \
  -backend-config=key=v4/service-billing/dev/terraform.tfstate

# Plan
terraform plan \
  -var-file=terraform.tfvars.dev \
  -var-file=secret.tfvars \
  -out=dev.tfplan

# Apply
terraform apply dev.tfplan
```

### Exemplo: Deploy em Homologation

```bash
terraform init \
  -backend-config=key=v4/service-billing/homologation/terraform.tfstate

terraform apply \
  -var-file=terraform.tfvars.homologation \
  -var-file=secret.tfvars \
  -auto-approve
```

### Exemplo: Deploy em Production

```bash
terraform init \
  -backend-config=key=v4/service-billing/production/terraform.tfstate

terraform apply \
  -var-file=terraform.tfvars.production \
  -var-file=secret.tfvars \
  -auto-approve
```

## Variáveis

### Obrigatórias (sem defaults)
- `environment` - dev | homologation | production

### Sensíveis (em secret.tfvars)
- `aws_access_key`
- `aws_secret_key`
- `mercadopago_access_token`

### Com Defaults
- `region` = "us-east-2"
- `project_name` = "challengeone"
- `service_name` = "billing"
- `app_replicas` = 2
- `app_image` = "thiagotierre/billing-service:latest"
- `app_port` = 8080
- `cpu_request` = "250m"
- `cpu_limit` = "500m"
- `memory_request` = "512Mi"
- `memory_limit` = "1Gi"
- `mercadopago_public_key` = "APP_USR-test-public-key"
- `sqs_queue_url` = ""
- `hpa_min_replicas` = 2
- `hpa_max_replicas` = 4
- `hpa_cpu_threshold` = 70
- `eks_state_key` = "v4/kubernetes/dev/terraform.tfstate"
- `dynamodb_state_key` = "v4/dynamodb-billing/dev/terraform.tfstate"

## Customização

Para alterar valores padrão (ex: aumentar replicas), edite `variables.tf` e aumente o `default` correspondente. Não altere `terraform.tfvars.*` - esses arquivos devem conter apenas `environment`.

## Outputs

```bash
terraform output

namespace              - Kubernetes namespace (payment-service)
service_name           - Kubernetes service name (billing)
service_cluster_ip     - ClusterIP interno
loadbalancer_hostname  - DNS externo (se aplicável)
deployment_name        - Kubernetes deployment name
```
