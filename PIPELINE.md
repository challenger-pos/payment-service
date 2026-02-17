# GitHub Actions CI/CD Pipeline - Billing Service

## 🚀 Pipeline Overview

Este pipeline automatiza o build, teste, análise de qualidade e deploy da aplicação Billing Service em 3 ambientes (Development, Homologation, Production) usando:

- **Build**: Maven 3.9+ com Java 21
- **Tests**: JUnit + Spring Test (Unit + Integration)
- **Quality**: SonarQube Cloud com Quality Gate
- **Security**: Trivy para análise de vulnerabilidades na imagem Docker
- **Registry**: Docker Hub
- **Infrastructure**: AWS EKS + Terraform
- **Orchestration**: Kubernetes com HPA

---

## 📊 Pipeline Stages

```
┌─────────────────────────────────────────────────────────────────┐
│ TRIGGER: Push to main / Pull Request                            │
└────────┬────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│ 1. BUILD (Always)                                               │
│    └─ mvn clean package -DskipTests                            │
└────────┬────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. TEST (Depends on BUILD)                                      │
│    ├─ Unit Tests: mvn test                                      │
│    └─ Integration Tests: mvn failsafe:integration-test          │
███████████ Coverage Report: JaCoCo (80% threshold)               │
└────────┬────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. QUALITY CHECK (Depends on TEST)                              │
│    └─ SonarQube: mvn sonar:sonar                                │
███████████ Quality Gate: Coverage >= 80%, No Blockers            │
└────────┬────────────────────────────────────────────────────────┘
         │
         ▼ (Only on main branch)
┌─────────────────────────────────────────────────────────────────┐
│ 4. DOCKER BUILD (Depends on TEST)                               │
│    └─ Build: docker build -t docker-hub-user/billing:SHA       │
│    └─ Push: latest + SHA tags                                   │
│    └─ Cache: GitHub Actions cache                               │
└────────┬────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│ 5. SECURITY SCAN (Depends on DOCKER BUILD)                      │
│    └─ Trivy: Scan para CVEs na imagem                          │
│    └─ Report: SARIF upload para GitHub Security tab            │
└────────┬────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│ 6. DEPLOY DEV (Environment: development)                        │
│    ├─ Terraform Init/Plan/Apply (development)                   │
│    ├─ kubectl rollout status                                    │
│    └─ Health Check: 5 minutes (30 attempts × 10s)               │
└────────┬────────────────────────────────────────────────────────┘
         │
         ├─ Wait 5 minutes
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│ 7. DEPLOY HOMOLOG (Environment: homologation)                   │
│    ├─ Terraform Init/Plan/Apply (homologation)                  │
│    ├─ kubectl rollout status                                    │
│    └─ Health Check: 10 minutes (60 attempts × 10s)              │
└────────┬────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│ 8. DEPLOY PROD (Environment: production)                        │
│    ✋ REQUIRES: Manual Approval (2 reviewers)                   │
│    ├─ Terraform Init/Plan/Apply (production)                    │
│    ├─ kubectl rollout status                                    │
│    ├─ Health Check: 15 minutes (90 attempts × 10s)              │
│    └─ Auto Rollback: Se health check falhar                     │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔐 Required Secrets

Todos os secrets abaixo devem ser configurados em **Settings > Secrets and variables > Actions**:

| Secret                | Description         | Source                             |
| --------------------- | ------------------- | ---------------------------------- |
| `DOCKER_HUB_USERNAME` | Docker Hub username | Docker Hub → Account               |
| `DOCKER_HUB_TOKEN`    | Docker Hub token    | Docker Hub → Security              |
| `SONAR_TOKEN`         | SonarCloud token    | SonarCloud → My Account → Security |
| `SONAR_ORGANIZATION`  | SonarCloud org key  | SonarCloud → Organization          |
| `SONAR_HOST_URL`      | SonarQube URL       | https://sonarcloud.io              |
| `AWS_ROLE_TO_ASSUME`  | IAM Role ARN        | AWS IAM Console                    |
| `AWS_REGION`          | AWS Region          | us-east-1 (default)                |
| `TF_BACKEND_BUCKET`   | S3 bucket for state | AWS S3 Console                     |
| `KUBECONFIG`          | Base64 kubeconfig   | `base64 -w0 ~/.kube/config`        |

Para setup completo, veja [CI_CD_SETUP.md](./CI_CD_SETUP.md).

---

## 📋 Branch Protection Rules

Configure em: **Settings > Branches > Add rule**

**Branch Protection für `main`:**

```yaml
Branch name pattern: main

Required status checks:
  - Build Application ✓
  - Automated Tests ✓
  - SonarQube Quality Gate ✓
  - Build and Push Docker Image ✓

Additional rules:
  - Require pull request before merging ✓
  - Require code owner approval ✓
  - Require 2 approvals ✓
  - Include administrators ✓
```

---

## 🎯 GitHub Environments

| Environment  | Reviewers | Auto Deploy | Approval            |
| ------------ | --------- | ----------- | ------------------- |
| development  | None      | ✅ Yes      | -                   |
| homologation | 1         | ✅ Yes      | Manual wait         |
| production   | 2         | ❌ No       | **Manual Approval** |

**Note:** Production deployments requerem aprovação de 2 reviewers.

---

## 🧪 Local Testing

### Build and Test Locally

```bash
# Clone repository
git clone <repo-url>
cd billing-service

# Setup Java 21
# (using SDKMAN or system package manager)

# Build application
mvn clean package

# Run tests
mvn test                           # Unit tests
mvn failsafe:integration-test     # Integration tests

# Check coverage
mvn jacoco:report
open target/site/jacoco/index.html
```

### Run SonarQube Analysis Locally

```bash
mvn clean verify \
  -Pcoverage \
  org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
  -Dsonar.projectKey=fiap_billing-service \
  -Dsonar.organization=your-org \
  -Dsonar.host.url=https://sonarcloud.io \
  -Dsonar.login=YOUR_SONAR_TOKEN
```

### Build Docker Image Locally

```bash
# Build without push
docker build -t billing-service:test .

# Test container
docker run -p 8080:8080 billing-service:test

# Check health
curl http://localhost:8080/actuator/health
```

---

## ⚡ Triggering the Pipeline

### Via GitHub Web Interface

```
1. Go to Repository → Actions → CI/CD Pipeline
2. Click "Run workflow"
3. Select branch (default: main)
4. Click "Run workflow"
```

### Via GitHub CLI

```bash
# List all workflows
gh workflow list

# Trigger CI/CD workflow
gh workflow run ci-cd.yml

# Trigger with specific branch
gh workflow run ci-cd.yml -r develop
```

### Automatic Triggers

| Event        | Branch  | Action                       |
| ------------ | ------- | ---------------------------- |
| Push         | main    | Full pipeline (All stages)   |
| Push         | develop | Up to Deploy Dev             |
| Pull Request | \*      | Build + Test + Quality Check |

---

## 📊 Monitoring & Results

### GitHub Actions Dashboard

```
Repository → Actions → CI/CD Pipeline

View:
├─ Workflow runs
├─ Job logs
├─ Artifact downloads
└─ Status badges
```

### Test Reports

```
After test job completes:
├─ GitHub Actions tab: "Test Results"
├─ Artifacts:
│  ├─ test-reports/
│  ├─ target/site/jacoco/ (coverage)
│  └─ target/site/ (full report)
└─ PR comments: Shows SonarQube analysis
```

### SonarQube Dashboard

```
SonarCloud → Projects → billing-service

Monitor:
├─ Coverage: Trend over time
├─ Quality Gate: Status
├─ Reliability: Issues by severity
├─ Security: Vulnerabilities
└─ Code hotspots: Areas of concern
```

### Kubernetes Deployment Status

```bash
# Check deployment status
kubectl rollout status deployment/billing-service \
  -n billing-development \
  --timeout=5m

# View pod logs
kubectl logs -n billing-development \
  -l app=billing-service -f

# Port forward for testing
kubectl port-forward -n billing-development \
  svc/billing-service 8080:8080
```

---

## 🔄 Rollback Procedures

### Manual Rollback to Previous Version

```bash
# 1. Get previous deployment
kubectl rollout history deployment/billing-service \
  -n billing-production

# 2. Rollback to previous revision
kubectl rollout undo deployment/billing-service \
  -n billing-production

# 3. Monitor rollback progress
kubectl rollout status deployment/billing-service \
  -n billing-production --timeout=5m
```

### Automatic Rollback (Production Only)

- **Triggered by:** Health check failure after 15 minutes
- **Action:** Revert to previous Docker image tag
- **Validation:** Performs health check again
- **Result:** Comment on PR with status

---

## 🐛 Troubleshooting

### ❌ Build Fails

```bash
# Check logs
# GitHub Actions → Your run → "Build Application" job

# Fix:
# 1. Verify Java 21 compatibility
# 2. Check Maven dependencies
# 3. Run locally: mvn clean package
```

### ❌ Tests Fail

```bash
# Check logs
# GitHub Actions → Your run → "Automated Tests" job

# Fix:
# 1. Run locally: mvn test
# 2. Check PostgreSQL health (Docker)
# 3. Review test output in PR
```

### ❌ SonarQube Quality Gate Fails

```bash
# Check SonarCloud dashboard for:
# - Coverage < 80%: Add more tests
# - Security issues: Fix vulnerabilities
# - Code smells: Refactor code

# Fix:
# 1. Increase test coverage: mvn jacoco:report
# 2. Resolve security issues
# 3. Re-push changes
```

### ❌ Docker Push Fails

```bash
# Likely cause: Invalid Docker Hub credentials

# Fix:
# 1. Verify DOCKER_HUB_USERNAME secret
# 2. Verify DOCKER_HUB_TOKEN secret
# 3. Test locally: docker login
# 4. Regenerate token in Docker Hub if needed
```

### ❌ Deploy Fails

```bash
# Check logs
# GitHub Actions → Your run → "Deploy [Env]" job

# Common causes:
# 1. AWS credentials invalid: Check AWS_ROLE_TO_ASSUME
# 2. EKS cluster unreachable: Verify cluster exists
# 3. Terraform error: Check tfplan output
# 4. Health check timeout: Check pod logs

# Fix:
# 1. kubectl describe pod <pod-name> -n billing-<env>
# 2. kubectl logs <pod-name> -n billing-<env>
# 3. Check Terraform backend state file
```

---

## 📚 Documentation

| Document                                                     | Purpose                                                    |
| ------------------------------------------------------------ | ---------------------------------------------------------- |
| [CI_CD_SETUP.md](./CI_CD_SETUP.md)                           | **Complete setup guide** for all secrets and prerequisites |
| [README.md](./README.md)                                     | Application documentation                                  |
| [.github/workflows/ci-cd.yml](./.github/workflows/ci-cd.yml) | **Pipeline definition** (YAML)                             |
| [pom.xml](./pom.xml)                                         | Maven configuration with plugins                           |
| [Dockerfile](./Dockerfile)                                   | Container build definition                                 |

---

## 🎓 Best Practices

### ✅ DO

```bash
# 1. Write tests alongside code
mvn test          # Run before committing

# 2. Review SonarQube analysis
# Check PR comment for analysis results

# 3. Keep commits small and focused
git commit -m "feat: add payment validation"

# 4. Merge only when ready for production
# All checks must pass before merging to main

# 5. Monitor deployments
kubectl logs -n billing-<env> -l app=billing-service -f
```

### ❌ DON'T

```bash
# 1. Commit without testing
# Always run: mvn test failsafe:integration-test

# 2. Push to main directly
# Use pull requests and require reviews

# 3. Bypass Quality Gate
# Fix code issues instead of ignoring warnings

# 4. Commit secrets
# Use GitHub Secrets, never hardcode!

# 5. Deploy to production manually
# Always use the GitHub Actions pipeline
```

---

## 📈 Metrics & KPIs

Monitor these metrics to optimize the pipeline:

- **Build Time:** Target < 5 minutes
- **Test Coverage:** Maintain >= 80%
- **Quality Gate:** 100% pass rate
- **Deployment Success:** Target 99%+
- **Rollback Frequency:** Target < 1% of deployments
- **Release Frequency:** Main branch → Production (on-demand)

---

## 🔗 References

- [GitHub Actions Docs](https://docs.github.com/en/actions)
- [Maven Documentation](https://maven.apache.org/guides/)
- [SonarCloud Docs](https://docs.sonarcloud.io/)
- [Kubernetes Deployment](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/)
- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)

---

**Last Updated:** February 17, 2026  
**Maintainers:** DevOps Team
