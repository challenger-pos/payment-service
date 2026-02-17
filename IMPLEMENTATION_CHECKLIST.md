# 🚀 CI/CD Implementation Checklist

Complete este checklist para configurar e validar o pipeline CI/CD do Billing Service.

---

## Phase 1: Local Validation (No GitHub access needed)

- [ ] **Build locally**
  ```bash
  cd billing-service
  mvn clean package
  ```
- [ ] **Run unit tests locally**
  ```bash
  mvn test
  ```
- [ ] **Check test coverage locally**

  ```bash
  mvn jacoco:report
  open target/site/jacoco/index.html  # Mac
  # ou
  xdg-open target/site/jacoco/index.html  # Linux
  ```

  - [ ] Coverage >= 80%

- [ ] **Build Docker image locally**

  ```bash
  docker build -t billing-service:test .
  ```

- [ ] **Test Docker image**
  ```bash
  docker run -p 8080:8080 billing-service:test
  curl http://localhost:8080/actuator/health
  ```

---

## Phase 2: SonarCloud Setup

- [ ] **Create SonarCloud account**
  - [ ] Visit https://sonarcloud.io
  - [ ] Sign up with GitHub account
  - [ ] Link GitHub organization

- [ ] **Create SonarCloud organization**
  - [ ] Dashboard → Create Organization
  - [ ] Link GitHub organization
  - [ ] Grant necessary permissions

- [ ] **Create billing-service project**
  - [ ] Projects → Create Project
  - [ ] Select GitHub organization
  - [ ] Select `organization` repository
  - [ ] Configure branches (main as primary)
- [ ] **Configure Quality Gate**
  - [ ] Project Settings → Quality Gates
  - [ ] Set minimum coverage: 80%
  - [ ] Set duplicate lines: < 5%
  - [ ] Set blockers: 0
  - [ ] Set critical issues: 0

- [ ] **Generate SONAR_TOKEN**
  - [ ] Click on profile icon (top right)
  - [ ] Account → Security
  - [ ] Generate New Token
  - [ ] Copy token (will not show again)

- [ ] **Verify project key**
  - [ ] Settings → General
  - [ ] Note the Project Key (format: `github_ORG_billing-service`)
  - [ ] Update `pom.xml` if needed

---

## Phase 3: Docker Hub Setup

- [ ] **Create Docker Hub account**
  - [ ] Visit https://hub.docker.com
  - [ ] Sign up or login

- [ ] **Create Access Token**
  - [ ] Account → Professional Settings → Security
  - [ ] New Access Token
  - [ ] Token description: `github-actions-billing-service`
  - [ ] Permissions: Read & Write
  - [ ] Copy token (will not show again)

- [ ] **Create public repository** (optional)
  - [ ] Create → Repository
  - [ ] Repository name: `billing-service`
  - [ ] Visibility: Public
  - [ ] Short description: "Billing microservice with Mercado Pago integration"

---

## Phase 4: AWS Configuration

### AWS OIDC Setup (Recomendado)

- [ ] **Create OIDC Provider**

  ```bash
  aws iam create-open-id-connect-provider \
    --url https://token.actions.githubusercontent.com \
    --client-id-list sts.amazonaws.com \
    --thumbprint-list 1b511abead59c6ce207077c0ef0cae8f148d8e93 \
    --region us-east-1
  ```

- [ ] **Create IAM Role: `github-actions-billing-service`**
  - [ ] Service role for GitHub Actions
  - [ ] Trust entity: OIDC provider (token.actions.githubusercontent.com)
  - [ ] Subject filtering:
    ```
    token.actions.githubusercontent.com:sub
    = repo:YOUR_ORG/organization:ref:refs/heads/main
    ```

- [ ] **Attach permissions to role**
  - [ ] AmazonEKSClusterPolicy
  - [ ] AmazonECS_FullAccess (for container operations)
  - [ ] Inline policy for S3 (Terraform backend):
    ```json
    {
      "Version": "2012-10-17",
      "Statement": [
        {
          "Effect": "Allow",
          "Action": ["s3:*", "dynamodb:*"],
          "Resource": [
            "arn:aws:s3:::billing-service-terraform-state-*",
            "arn:aws:dynamodb:*:*:table/terraform-locks"
          ]
        }
      ]
    }
    ```

- [ ] **Note the Role ARN**
  - [ ] Format: `arn:aws:iam::123456789012:role/github-actions-billing-service`

### Alternative: Access Keys (Less Secure)

- [ ] **Skip if using OIDC above**
- [ ] Create IAM user or use existing
- [ ] Create Access Key ID
- [ ] Copy Secret Access Key

### S3 Terraform Backend

- [ ] **Create S3 bucket for Terraform state**

  ```bash
  aws s3api create-bucket \
    --bucket billing-service-terraform-state-prod \
    --region us-east-1
  ```

- [ ] **Enable versioning**

  ```bash
  aws s3api put-bucket-versioning \
    --bucket billing-service-terraform-state-prod \
    --versioning-configuration Status=Enabled
  ```

- [ ] **Enable encryption**

  ```bash
  aws s3api put-bucket-encryption \
    --bucket billing-service-terraform-state-prod \
    --server-side-encryption-configuration '{
      "Rules": [{
        "ApplyServerSideEncryptionByDefault": {"SSEAlgorithm": "AES256"}
      }]
    }'
  ```

- [ ] **Create DynamoDB table for locks**
  ```bash
  aws dynamodb create-table \
    --table-name terraform-locks \
    --attribute-definitions AttributeName=LockID,AttributeType=S \
    --key-schema AttributeName=LockID,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST \
    --region us-east-1
  ```

### Verify EKS Clusters

- [ ] **Development cluster exists**

  ```bash
  aws eks describe-cluster --name billing-service-dev --region us-east-1
  ```

- [ ] **Homologation cluster exists**

  ```bash
  aws eks describe-cluster --name billing-service-homolog --region us-east-1
  ```

- [ ] **Production cluster exists**
  ```bash
  aws eks describe-cluster --name billing-service-prod --region us-east-1
  ```

---

## Phase 5: GitHub Configuration

### Repository Settings

- [ ] **Go to repository: Settings**

- [ ] **Configure Secrets** (Settings → Secrets and variables → Actions)

  **Docker Hub Secrets:**
  - [ ] `DOCKER_HUB_USERNAME` = your_dockerhub_username
  - [ ] `DOCKER_HUB_TOKEN` = your_access_token

  **SonarQube Secrets:**
  - [ ] `SONAR_TOKEN` = your_sonar_token
  - [ ] `SONAR_ORGANIZATION` = your_sonarcloud_org
  - [ ] `SONAR_HOST_URL` = https://sonarcloud.io

  **AWS Secrets:**
  - [ ] `AWS_ROLE_TO_ASSUME` = arn:aws:iam::xxx:role/github-actions-billing-service
  - [ ] `AWS_REGION` = us-east-1
  - [ ] `TF_BACKEND_BUCKET` = billing-service-terraform-state-prod

  **Or use existing keys:**
  - [ ] `AWS_ACCESS_KEY_ID` = your_key_id (if not using OIDC)
  - [ ] `AWS_SECRET_ACCESS_KEY` = your_secret (if not using OIDC)

### Branch Protection Rules

- [ ] **Navigate to:** Settings → Branches → Add rule

- [ ] **Branch name pattern:** `main`

- [ ] **Require pull request before merging**
  - [ ] Require a pull request before merging
  - [ ] Dismiss stale pull request approvals
  - [ ] Require code owner approval (if applicable)
  - [ ] Require conversation resolution before merging

- [ ] **Require status checks to pass**
  - [ ] Require branches to be up to date before merging
  - [ ] Select these required status checks:
    - [ ] Build Application
    - [ ] Automated Tests
    - [ ] SonarQube Quality Gate
    - [ ] Build and Push Docker Image

- [ ] **Restrict who can push to matching branches**
  - [ ] Include administrators

### Create Environments

- [ ] **Development environment**
  - [ ] Settings → Environments → New environment
  - [ ] Name: `development`
  - [ ] No protection rules (auto-deploy)

- [ ] **Homologation environment**
  - [ ] Settings → Environments → New environment
  - [ ] Name: `homologation`
  - [ ] Protection rules:
    - [ ] Selected branches: main
    - [ ] Required reviewers: 1

- [ ] **Production environment**
  - [ ] Settings → Environments → New environment
  - [ ] Name: `production`
  - [ ] Protection rules:
    - [ ] Selected branches: main
    - [ ] Required reviewers: 2
    - [ ] Wait timer: 15 minutes
    - [ ] Deployment history: GitHub Actions only

---

## Phase 6: Git & Push

- [ ] **Commit changes to feature branch**

  ```bash
  cd /home/workstation/Documents/projects/pos/organization/billing-service

  # Verify you're on feat/ci-cd branch
  git branch

  # Add all new files
  git add .github/
  git add pom.xml
  git add terraform/
  git add *.md

  # Commit
  git commit -m "feat: add complete CI/CD pipeline

  - GitHub Actions workflow: Build → Test → Quality → Docker → Deploy
  - Maven plugins: Surefire, Failsafe, JaCoCo, SonarQube
  - Terraform variables for dynamic image deployment
  - Complete documentation: PIPELINE.md, CI_CD_SETUP.md
  - SonarCloud integration with Quality Gate
  - Automated deployment to Dev → Homolog → Prod
  - Health checks and automatic rollback in production"

  # Push to feature branch
  git push origin feat/ci-cd
  ```

---

## Phase 7: First Pipeline Run

- [ ] **Create Pull Request**
  - [ ] Go to GitHub repository
  - [ ] Create Pull Request: `feat/ci-cd` → `main`
  - [ ] Title: "Implement CI/CD Pipeline"
  - [ ] Wait for status checks to complete

- [ ] **Review GitHub Actions run**
  - [ ] Repository → Actions → CI/CD Pipeline
  - [ ] Click latest run
  - [ ] Verify all stages:
    - [ ] Build ✓
    - [ ] Test ✓
    - [ ] Quality Check ✓
    - [ ] Docker Build: SKIPPED (PR, not main)

- [ ] **Review test reports**
  - [ ] Click "Test Results" tab
  - [ ] Verify all tests passed

- [ ] **Review SonarQube analysis**
  - [ ] Look for PR comment with SonarQube results
  - [ ] Check Quality Gate status: PASSED
  - [ ] Coverage: Check if >= 80%

- [ ] **Fix any issues found**
  - [ ] If Quality Gate failed: Add tests or fix code smells
  - [ ] If tests failed: Debug and fix
  - [ ] Commit fixes: `git commit -am "fix: improve code quality"`
  - [ ] Push: `git push origin feat/ci-cd`

- [ ] **Merge Pull Request**
  - [ ] Wait for all reviewers to approve
  - [ ] All status checks must pass
  - [ ] Merge via "Squash and merge"

---

## Phase 8: Validate Production Pipeline

- [ ] **Monitor Actions run after merge**
  - [ ] Repository → Actions → CI/CD Pipeline
  - [ ] Wait for main branch run to complete
  - [ ] Should see:
    - [ ] Build ✓
    - [ ] Test ✓
    - [ ] Quality Check ✓
    - [ ] Build and Push Docker Image ✓
    - [ ] Security Scan ✓
    - [ ] Deploy Dev ✓
    - [ ] Deploy Homolog (after 5-min wait) ✓
    - [ ] Deploy Prod (WAITING FOR APPROVAL)

- [ ] **Approve Production Deployment** (if desired)
  - [ ] Repository → Actions → CI/CD Pipeline
  - [ ] Click latest run
  - [ ] Scroll to "Deploy to Production"
  - [ ] Click "Review deployments"
  - [ ] Select "production"
  - [ ] Comment (optional): "Ready for production"
  - [ ] Click "Approve and deploy"

- [ ] **Monitor Production Deployment**
  - [ ] Wait for deployment to complete
  - [ ] Check health checks pass
  - [ ] Verify no automatic rollback occurred

---

## Phase 9: Validation & Testing

- [ ] **Verify Docker image in Docker Hub**

  ```bash
  docker pull docker.io/YOUR_USER/billing-service:latest
  docker images | grep billing-service
  ```

- [ ] **Verify EKS deployment**

  ```bash
  # For each environment:
  kubectl get deployment -n billing-development
  kubectl get pods -n billing-development
  kubectl logs -n billing-development -l app=billing-service -f
  ```

- [ ] **Test application endpoints**

  ```bash
  # Port forward
  kubectl port-forward -n billing-development \
    svc/billing-service 8080:8080

  # Test health
  curl http://localhost:8080/actuator/health
  ```

- [ ] **Monitor SonarQube metrics**
  - [ ] SonarCloud → Projects → billing-service
  - [ ] Verify:
    - [ ] Coverage >= 80%
    - [ ] 0 Bugs
    - [ ] 0 Vulnerabilities
    - [ ] 0 Blocker issues

- [ ] **Check GitHub Actions metrics**
  - [ ] Settings → Actions → Runners (if self-hosted)
  - [ ] Repository → Actions
  - [ ] Track:
    - [ ] Number of successful builds
    - [ ] Average build time
    - [ ] Test pass rate

---

## Phase 10: Documentation & Training

- [ ] **Review documentation**
  - [ ] [PIPELINE.md](./PIPELINE.md) - Overview
  - [ ] [CI_CD_SETUP.md](./CI_CD_SETUP.md) - Setup guide
  - [ ] [.github/workflows/README.md](./.github/workflows/README.md) - Technical details

- [ ] **Update team documentation**
  - [ ] Share setup guide with team
  - [ ] Train on deployment process
  - [ ] Document common issues and solutions

- [ ] **Update main README.md**
  - [ ] ✓ Already updated in this implementation

---

## Phase 11: Ongoing Maintenance

- [ ] **Monitor metrics weekly**
  - [ ] Build success rate
  - [ ] Test coverage trends
  - [ ] Deployment frequency
  - [ ] Rollback frequency

- [ ] **Update documentation quarterly**
  - [ ] GitHub Actions version updates
  - [ ] SonarCloud new features
  - [ ] AWS updates

- [ ] **Review security regularly**
  - [ ] Update Docker base images
  - [ ] Review Trivy scan results
  - [ ] Rotate secrets annually

- [ ] **Optimize performance**
  - [ ] Profile build times
  - [ ] Optimize cache strategies
  - [ ] Review test execution times

---

## ✅ Final Verification

After completing all phases, verify:

```bash
# 1. Check all files exist
ls -la .github/workflows/ci-cd.yml
ls -la .github/workflows/README.md
ls -la terraform/variables.tf
ls -la terraform/outputs.tf
ls -la CI_CD_SETUP.md
ls -la PIPELINE.md

# 2. Verify pom.xml has plugins
grep "maven-surefire-plugin" pom.xml
grep "maven-failsafe-plugin" pom.xml
grep "jacoco-maven-plugin" pom.xml
grep "sonar-maven-plugin" pom.xml

# 3. Check Git status
git status  # Should show all new files ready to commit

# 4. Verify no secrets in code
grep -r "token" --include="*.yml" --include="*.yaml"
grep -r "password" --include="*.yml" --include="*.yaml"
# Should find NO secrets in files (only in ${{ secrets.* }})
```

---

## 🎉 Success Criteria

Pipeline is ready when:

✅ All phases 1-5 completed without errors
✅ First PR run shows Build, Test, Quality stages passing
✅ First main branch merge triggers full pipeline
✅ Docker image pushed to Docker Hub
✅ All environments deployed successfully
✅ Health checks passing in all environments
✅ SonarCloud shows quality metrics
✅ No secrets exposed in code or logs
✅ Team trained on deployment process

---

## 📞 Troubleshooting Reference

| Issue             | Solution                       | Docs                                                                                                 |
| ----------------- | ------------------------------ | ---------------------------------------------------------------------------------------------------- |
| Build fails       | Check Maven compilation errors | [CI_CD_SETUP.md#troubleshooting](./CI_CD_SETUP.md#-troubleshooting)                                  |
| Test fails        | Check test database connection | [PIPELINE.md#-trigger-the-pipeline](./PIPELINE.md#-triggering-the-pipeline)                          |
| SonarQube fails   | Verify SONAR_TOKEN             | [CI_CD_SETUP.md#sonarqube-cloud-integration](./CI_CD_SETUP.md#sonarqube-cloud-integration)           |
| Docker push fails | Check DOCKER_HUB_TOKEN         | [CI_CD_SETUP.md#docker-hub-integration](./CI_CD_SETUP.md#docker-hub-integration)                     |
| Deploy fails      | Check AWS credentials          | [CI_CD_SETUP.md#aws-integration-oidc-recomendado](./CI_CD_SETUP.md#aws-integration-oidc-recomendado) |

---

## 📚 Documentation Map

```
billing-service/
├── README.md                          ← Start here
├── PIPELINE.md                        ← Pipeline overview
├── CI_CD_SETUP.md                     ← Complete setup guide
├── IMPLEMENTATION_CHECKLIST.md        ← This file
├── .github/
│   └── workflows/
│       ├── ci-cd.yml                  ← Workflow definition
│       └── README.md                  ← Job details
├── pom.xml                            ← Maven plugins
└── terraform/
    ├── variables.tf                   ← CI/CD variables
    ├── outputs.tf                     ← Deployment outputs
    └── terraform.tfvars.example       ← Example config
```

---

**Created:** February 17, 2026  
**Status:** Ready for implementation  
**Version:** 1.0
