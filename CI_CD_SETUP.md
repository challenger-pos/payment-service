# CI/CD Pipeline Setup Guide

Este documento descreve como configurar completamente o pipeline CI/CD para o Billing Service.

## 📋 Overview

O pipeline implementado possui 8 stages:

1. **Build** - Build Maven da aplicação
2. **Test** - Execução de testes automatizados (unit + integration)
3. **Quality Check** - Análise SonarQube com Quality Gate
4. **Docker Build** - Build e push da imagem Docker
5. **Security Scan** - Scan de vulnerabilidades com Trivy
6. **Deploy Dev** - Deploy automático em desenvolvimento
7. **Deploy Homolog** - Deploy automático em homologação (5 min após Dev)
8. **Deploy Prod** - **Deploy manual com aprovação** em produção

## 🔐 GitHub Secrets - Setup Obrigatório

Todos os seguintes secrets devem ser configurados em: **Settings > Secrets and variables > Actions**

### Docker Hub Integration

```
DOCKER_HUB_USERNAME
├─ Descrição: Seu usuário no Docker Hub
└─ Valor: seu_usuario_dockerhub

DOCKER_HUB_TOKEN
├─ Descrição: Token de autenticação do Docker Hub
├─ Como gerar: Docker Hub → Account Settings → Security → New Access Token
└─ Permissões: Read & Write

```

### AWS Integration (OIDC Recomendado)

**Opção 1: Using GitHub OpenID Connect (RECOMENDADO - mais seguro)**

```
AWS_ROLE_TO_ASSUME
├─ Descrição: ARN da role IAM para assumir
├─ Formato: arn:aws:iam::123456789012:role/github-actions-billing-service
└─ Permissões: EKS, Terraform backend access
```

**Setup AWS OIDC (uma única vez):**

```bash
# 1. Criar o OIDC provider no AWS
aws iam create-open-id-connect-provider \
  --url https://token.actions.githubusercontent.com \
  --client-id-list sts.amazonaws.com \
  --thumbprint-list hv8fX7xz1H5X6A9Xy9xz1H5X6A9Xy9xz1H5X6A9

# 2. Criar IAM Role: github-actions-billing-service
# Trust Policy:
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::123456789012:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com",
          "token.actions.githubusercontent.com:sub": "repo:YOUR_GITHUB_ORG/organization:ref:refs/heads/main"
        }
      }
    }
  ]
}

# 3. Attach permissions: AmazonEKSClusterPolicy, AmazonECSTaskExecutionRolePolicy
# 4. Adicionar inline policy para Terraform backend:
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject",
        "s3:GetObjectVersion",
        "dynamodb:PutItem",
        "dynamodb:GetItem",
        "dynamodb:DeleteItem",
        "dynamodb:DescribeTable"
      ],
      "Resource": [
        "arn:aws:s3:::your-terraform-bucket/*",
        "arn:aws:dynamodb:*:123456789012:table/terraform-locks"
      ]
    }
  ]
}
```

**Opção 2: Access Keys diretas (menos seguro)**

```
AWS_ACCESS_KEY_ID
└─ Descrição: Access Key ID da sua conta AWS

AWS_SECRET_ACCESS_KEY
└─ Descrição: Secret Access Key da sua conta AWS
```

### SonarQube Cloud Integration

```
SONAR_TOKEN
├─ Descrição: Token de autenticação do SonarQube Cloud
├─ Como gerar: SonarCloud → My Account → Security → Generate Token
└─ Permissões: Scan and Analyze

SONAR_ORGANIZATION
├─ Descrição: Organization key no SonarCloud
└─ Formato: seu-org-key

SONAR_HOST_URL
├─ Descrição: URL do servidor SonarQube
├─ Para SonarCloud: https://sonarcloud.io
└─ Para self-hosted: https://seu-sonarqube.com
```

### AWS Terraform Backend

```
TF_BACKEND_BUCKET
├─ Descrição: Nome do bucket S3 para estado Terraform
├─ Formato: billing-service-terraform-state-prod
└─ Nota: Bucket deve ter versionamento habilitado

AWS_REGION
├─ Descrição: Região AWS para os deployments
└─ Valor: us-east-1 (ou a região desejada)
```

### Kubernetes Access (opcional se usando OIDC)

```
KUBECONFIG
├─ Descrição: Configuração b64 do kubeconfig
├─ Como gerar: base64 -w0 ~/.kube/config | pbcopy
└─ Nota: Necessário para acesso direto ao cluster
```

## 🎯 Setup SonarCloud

### 1. Criar Organização no SonarCloud

```bash
# Acesse https://sonarcloud.io
# 1. Sign up com GitHub
# 2. Create Organization → Link GitHub Organization
# 3. Selecionar repositório "organization"
```

### 2. Criar Project

```bash
# No SonarCloud:
# 1. My Projects → Create Project
# 2. Select GitHub organization
# 3. Select "billing-service" repository
# 4. Project Key será: github_YOUR_ORG_billing-service
# ou customizar durante o setup
```

### 3. Configurar Quality Gate

```bash
# No SonarCloud → Settings → Quality Gates

Quality Gate Padrão (Recomendado):
├─ Coverage: >= 80%
├─ Duplicated Lines: < 5%
├─ Maintainability Rating: A
├─ Reliability Rating: A
├─ Security Rating: A
├─ Security Review Rating: A
└─ Blocker Issues: 0

# Ou criar custom:
SonarCloud → Quality Gates → Create
→ Set as default
```

### 4. Configurar Branch Analysis

```bash
# SonarCloud → Project Settings → Branches and Pull Requests
├─ Main branch: main
├─ Branches: Include all branches
├─ Pull Requests: Auto-provision
└─ Issues: Keep open
```

## 🏗️ AWS Infrastructure

### 1. EKS Clusters

Ensure você tem 3 clusters EKS:

```
Development:
├─ Cluster name: billing-service-dev
├─ Namespace: billing-development
└─ Instance type: t3.medium (auto-scaling 2-5)

Homologation:
├─ Cluster name: billing-service-homolog
├─ Namespace: billing-homologation
└─ Instance type: t3.large (auto-scaling 2-5)

Production:
├─ Cluster name: billing-service-prod
├─ Namespace: billing-production
└─ Instance type: t3.xlarge (auto-scaling 3-10)
```

### 2. S3 Backend Terraform

```bash
# Create bucket and configure:
aws s3api create-bucket \
  --bucket billing-service-terraform-state-prod \
  --region us-east-1

# Enable versioning:
aws s3api put-bucket-versioning \
  --bucket billing-service-terraform-state-prod \
  --versioning-configuration Status=Enabled

# Enable encryption:
aws s3api put-bucket-encryption \
  --bucket billing-service-terraform-state-prod \
  --server-side-encryption-configuration '{
    "Rules": [{
      "ApplyServerSideEncryptionByDefault": {
        "SSEAlgorithm": "AES256"
      }
    }]
  }'

# Block public access:
aws s3api put-public-access-block \
  --bucket billing-service-terraform-state-prod \
  --public-access-block-configuration \
    "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"
```

### 3. DynamoDB para Terraform Locks

```bash
aws dynamodb create-table \
  --table-name terraform-locks \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1
```

## 📝 Branch Protection Rules

Configure em: **Settings > Branches > Add rule**

```
Branch name pattern: main

Requirements:
├─ Require a pull request before merging
│  ├─ Dismiss stale pull request approvals: ✓
│  ├─ Require code owner approval: ✓
│  └─ Require status checks to pass: ✓
│
├─ Require status checks to pass before merging:
│  ├─ Build Application: ✓
│  ├─ Automated Tests: ✓
│  ├─ SonarQube Quality Gate: ✓
│  └─ Build and Push Docker Image: ✓
│
├─ Require dismissal of pull request reviews: ✓
│  └─ Dismiss pull request review restrictions: ✓
│
├─ Restrict who can push to matching branches: ❌
│
└─ Include administrators: ✓
```

## 🚀 GitHub Environments

Configure em: **Settings > Environments**

### Development

```
Environment name: development

Protection rules:
├─ Deployment branches: All branches
└─ Reviewers: None (optional)

Secrets (Inherit from repo level):
├─ AWS_ROLE_TO_ASSUME
└─ AWS_REGION
```

### Homologation

```
Environment name: homologation

Protection rules:
├─ Deployment branches: Selected branches
│  └─ main
└─ Reviewers: 1 required

Secrets (Inherit from repo level):
├─ AWS_ROLE_TO_ASSUME
└─ AWS_REGION
```

### Production

```
Environment name: production

Protection rules:
├─ Deployment branches: Selected branches
│  └─ main
├─ Reviewers: 2 required
├─ Deployment history: Only allow GitHub Actions
└─ Wait timer: 15 minutes before granting access

Secrets (Inherit from repo level):
├─ AWS_ROLE_TO_ASSUME
└─ AWS_REGION
```

## 🔄 Running the Pipeline Manually

### Trigger workflow via GitHub CLI

```bash
# Trigger pipeline para uma branch específica
gh workflow run ci-cd.yml -f environment=

# Trigger com uma ref específica
gh workflow run ci-cd.yml -r main

# Trigger deploy produção especificamente
gh workflow run ci-cd.yml -f environment=production -r main
```

### Ou via web interface

1. Go to: **Actions > CI/CD Pipeline**
2. Click: **Run workflow**
3. Select branch: **main**
4. Click: **Run workflow**

## 📊 Monitoring Execution

### 1. GitHub Actions

```
Repository → Actions → CI/CD Pipeline

View details:
├─ Build logs
├─ Test reports
├─ Coverage reports
├─ SonarQube results (comment PR)
└─ Deployment status
```

### 2. SonarQube

```
SonarCloud → Projects → billing-service

Monitor:
├─ Coverage: Deve aumentar com cada commit
├─ Quality Gate: Status de passou/falhou
├─ Pull Requests: Análise em cada PR
└─ Code hotspots: Areas problemáticas
```

### 3. AWS CloudWatch

```
EKS Deployment Logs:

# View deployment status
kubectl rollout status deployment/billing-service -n billing-development

# View pod logs
kubectl logs -n billing-development -l app=billing-service -f

# View metrics
kubectl top nodes
kubectl top pods -n billing-development
```

## ✅ Verification Checklist

Before running the pipeline, verify:

```
☐ DOCKER_HUB_USERNAME secret configured
☐ DOCKER_HUB_TOKEN secret configured
☐ AWS_ROLE_TO_ASSUME secret configured
☐ AWS_REGION secret configured
☐ TF_BACKEND_BUCKET secret configured
☐ SONAR_TOKEN secret configured
☐ SONAR_ORGANIZATION secret configured
☐ SONAR_HOST_URL secret configured

☐ SonarCloud account created
☐ SonarCloud organization linked
☐ SonarCloud project created
☐ SonarCloud quality gate configured

☐ AWS EKS clusters exist (dev, homolog, prod)
☐ S3 backend bucket created
☐ DynamoDB locks table created
☐ IAM role for GitHub Actions created

☐ Branch protection rules configured
☐ GitHub environments configured (dev, homolog, prod)
☐ Reviewers assigned to environments
☐ All secrets added to repository
```

## 🐛 Troubleshooting

### Build fails with "credentials not found"

```
Causa: Docker Hub credentials não configurados
Fix: Adicione DOCKER_HUB_USERNAME e DOCKER_HUB_TOKEN aos secrets
```

### SonarQube analysis fails

```
Causa: SONAR_TOKEN expirado ou inválido
Fix:
  1. Gere novo token em SonarCloud
  2. Atualize SONAR_TOKEN no GitHub secrets
```

### Terraform apply fails with state lock

```
Causa: DynamoDB locks table não existe
Fix:
  1. Crie a tabela DynamoDB conforme instruções acima
  2. Configure o backend.tf com lock settings
```

### Deploy hangs on health check

```
Causa: Pod não está iniciando corretamente
Fix:
  1. Verifique logs do pod: kubectl logs <pod-name> -n billing-development
  2. Verifique resources: kubectl describe pod <pod-name> -n billing-development
  3. Verifique liveness probe da aplicação
```

### Quality Gate not passing

```
Causa: Cobertura de código < 80% ou issues encontradas
Fix:
  1. Adicione testes para novas funcionalidades
  2. Resolva security/reliability issues no SonarQube
  3. Aumente cobertura de código conforme recomendações
```

## 📚 References

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [SonarCloud Documentation](https://docs.sonarcloud.io/)
- [AWS EKS Documentation](https://docs.aws.amazon.com/eks/)
- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)

---

**Last Updated:** February 17, 2026
