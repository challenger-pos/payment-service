# GitHub Actions Workflows

## Directory Structure

```
.github/
└── workflows/
    ├── ci-cd.yml                          # Main CI/CD pipeline
    └── README.md                          # This file
```

## ci-cd.yml - Complete Pipeline

### Overview

This workflow implements a comprehensive CI/CD pipeline with 8 stages:

```yaml
Events (Triggers):
├─ push: [main, master, develop]
├─ pull_request: [main, master, develop]
└─ workflow_dispatch: (manual with environment selection)
```

### Jobs Detail

#### 1. **build** - Build Application

```yaml
name: Build Application
runs-on: ubuntu-latest
```

**Purpose:** Compile and package the Spring Boot application

**Steps:**

- Checkout code (full history for SonarQube)
- Setup Java 21 with Maven caching
- Clean build and dependency resolution
- Package JAR artifact
- Upload artifact for downstream jobs

**Artifacts:**

- `application-jar`: Billing service JAR file

**Duration:** ~3-4 minutes

**Failure Condition:**

- Maven compilation errors
- Missing dependencies
- Memory issues (MAVEN_OPTS: -Xmx2048m)

---

#### 2. **test** - Automated Tests

```yaml
name: Automated Tests
runs-on: ubuntu-latest
needs: build
```

**Purpose:** Execute unit and integration tests with PostgreSQL

**Services:**

- PostgreSQL 17-alpine container (port 5432)
- Health check: `pg_isready`

**Steps:**

1. Run unit tests with JaCoCo coverage agent

   ```bash
   mvn test -DargLine="-javaagent:$JAVA_HOME/lib/jvm.jar"
   ```

2. Run integration tests

   ```bash
   mvn failsafe:integration-test
   ```

3. Generate SureFire + Failsafe reports

4. Publish GitHub native test results

**Coverage Tracking:**

- JaCoCo generates `target/site/jacoco/`
- Maven enforces 80% minimum coverage
- Reports published for PR review

**Artifacts:**

- `test-reports`: XML reports, coverage data
- `test-reports/target/site/`: HTML coverage reports

**Duration:** ~5-7 minutes

**Test Database:**

```
PostgreSQL:
├─ Database: billing_db_test
├─ User: billing_user
├─ Password: billing_password
└─ Port: 5432
```

---

#### 3. **quality-check** - SonarQube Analysis

```yaml
name: SonarQube Quality Gate
runs-on: ubuntu-latest
needs: test
```

**Purpose:** Perform static code analysis and enforce quality standards

**Prerequisites:**

- SonarCloud account (free tier available)
- Project created in SonarCloud
- SONAR_TOKEN generated and stored as secret

**Steps:**

1. Checkout code with full history
2. Setup Java 21
3. Run SonarQube analysis

   ```bash
   mvn clean verify -Pcoverage \
     org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
     -Dsonar.projectKey=fiap_billing-service \
     -Dsonar.login=$SONAR_TOKEN
   ```

4. Auto-comment PR with results (if PR)

**Quality Gate Checks:**

- Coverage: >= 80%
- Duplicated Lines: < 3%
- Code Smells: < threshold
- Bugs: = 0
- Vulnerabilities: = 0
- Security Hotspots: reviewed

**Outputs:**

- SonarCloud dashboard link
- PR comment with metrics
- Blocking result on failure
- Link to full analysis

**Duration:** ~3-4 minutes

**Configuration:**

```properties
sonar.projectKey=fiap_billing-service
sonar.projectName=Billing Service
sonar.organization=${SONAR_ORGANIZATION}
sonar.host.url=https://sonarcloud.io
sonar.sources=src/main
sonar.tests=src/test
sonar.coverage.exclusions=**/*Config.java,**/*Application.java
```

---

#### 4. **docker-build** - Docker Image Build & Push

```yaml
name: Build and Push Docker Image
runs-on: ubuntu-latest
needs: [build, test]
if: github.event_name == 'push' && github.ref == 'refs/heads/main'
```

**Purpose:** Build containerized application and push to Docker Hub

**Trigger:** Only on push to main branch (not PRs)

**Steps:**

1. Checkout code
2. Setup Docker Buildx (BuildKit for multi-platform)
3. Login to Docker Hub
   - Username: `DOCKER_HUB_USERNAME`
   - Token: `DOCKER_HUB_TOKEN`

4. Build and push with tags:

   ```
   docker.io/user/billing-service:latest
   docker.io/user/billing-service:SHA256
   ```

5. Use BuildKit cache for efficiency:
   - Cache from: GitHub Actions cache (prior builds)
   - Cache to: GitHub Actions cache (for future builds)

**Dockerfile Strategy:**

- Multi-stage build (Maven → JRE)
- Alpine Linux for minimal image
- Non-root user (appuser:1001)
- Health check via actuator endpoint

**Image Metrics:**

- Build size: ~200MB (optimized)
- Runtime size: ~100MB (JRE-only)

**Duration:** ~4-6 minutes

**Registry:** Docker Hub

- Repository: `{DOCKER_HUB_USERNAME}/billing-service`
- Tags: `latest`, `{commit-SHA}`

---

#### 5. **security-scan** - Trivy Vulnerability Scan

```yaml
name: Security Vulnerability Scan
runs-on: ubuntu-latest
needs: docker-build
if: github.event_name == 'push' && github.ref == 'refs/heads/main'
```

**Purpose:** Scan Docker image for known vulnerabilities

**Tool:** Trivy (Open-source vulnerability scanner)

**Scanning Target:**

```
docker.io/user/billing-service:SHA256
```

**Output Format:** SARIF (GitHub Security tab)

**Results:**

- GitHub Security → Code scanning
- Sorted by severity (Critical, High, Medium, Low)
- Actionable remediation steps
- CVE references and links

**Severity Levels:**

- ❌ **Critical**: Blocks deployment (if enabled)
- 🔴 **High**: Requires review
- 🟠 **Medium**: Monitor and plan
- 🟡 **Low**: Track for future

**Duration:** ~2-3 minutes

**Continue-on-error:** `true` (doesn't block pipeline)

---

#### 6. **deploy-dev** - Deploy to Development

```yaml
name: Deploy to Development
runs-on: ubuntu-latest
needs: [quality-check, docker-build, security-scan]
if: github.event_name == 'push' && github.ref == 'refs/heads/main'
environment: development
```

**Purpose:** Deploy application to development Kubernetes cluster

**AWS Access:** OIDC role assumption

**Terraform Operations:**

1. **terraform init**
   - Backend: S3 bucket + DynamoDB locks
   - Configuration: development environment

2. **terraform plan**
   - Variables:
     ```
     environment=development
     image_tag=${{ github.sha }}
     image_repository=docker.io/user/billing-service
     ```

3. **terraform apply**
   - Auto-approve (no manual intervention needed)
   - Updates EKS deployment

**Kubernetes Cluster:** `billing-service-dev`

- Namespace: `billing-development`
- Replicas: 2 (configurable via HPA)
- HPA: 2-10 replicas (70% CPU threshold)

**Post-Deployment Steps:**

1. Get kubeconfig from EKS
2. Wait for rollout completion (5 min timeout)
3. Health check loop:
   - Get pod name
   - Execute HTTP request to `/actuator/health`
   - Check for `"status":"UP"`
   - Retry 30 times × 10 seconds

**Health Check Response:**

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "sqs": { "status": "UP" },
    "messagingSystem": { "status": "UP" }
  }
}
```

**Duration:** ~8-10 minutes total

**Success Criteria:**

- Terraform apply succeeds
- Deployment rollout completes
- Health endpoint returns `"status":"UP"`

---

#### 7. **deploy-homolog** - Deploy to Homologation

```yaml
name: Deploy to Homologation
runs-on: ubuntu-latest
needs: deploy-dev
if: github.event_name == 'push' && github.ref == 'refs/heads/main'
environment: homologation
```

**Purpose:** Auto-deploy to homologation 5 minutes after dev

**Timing:**

- Waits 5 minutes after dev deployment
- Allows time for manual smoke tests on dev
- Provides burn-in period

**Kubernetes Cluster:** `billing-service-homolog`

- Namespace: `billing-homologation`
- Replicas: 2-5 (larger HPA range)

**Same as deploy-dev:**

- Terraform init/plan/apply
- Rollout status check (5 min)
- Health check (60 attempts × 10s)

**Environment Protection:**

- 1 reviewer required before deployment
- Wait timer configurable

**Duration:** ~8-10 minutes (plus 5-min wait)

---

#### 8. **deploy-prod** - Deploy to Production

```yaml
name: Deploy to Production
runs-on: ubuntu-latest
needs: [deploy-homolog]
if: github.event_name == 'push' && github.ref == 'refs/heads/main'
environment: production
```

**Purpose:** Deploy to production avec full rollback capability

**Protection:**

- ✋ **REQUIRES APPROVAL**
- 2 reviewers must approve
- 15-minute wait timer

**Kubernetes Cluster:** `billing-service-prod`

- Namespace: `billing-production`
- Replicas: 3-10 (largest HPA range)
- More aggressive monitoring

**Deployment Steps:**

1. AWS credentials setup (OIDC)
2. Terraform init (prod backend)
3. Terraform plan (review before apply)
4. Terraform apply
5. Rollout status (10 min timeout)
6. Extended health check (15 minutes = 90 attempts)

**Health Check (Extended):**

```bash
Loop 90 times, every 10 seconds:
  ├─ Get pod name (-l app=billing-service)
  ├─ Execute: kubectl exec pod -- curl /actuator/health
  ├─ Parse JSON response for "UP"
  └─ Success = exit loop, Failure = continue
```

**Automatic Rollback:**

- If health check fails → auto-rollback triggered
- Gets previous image tag from Terraform state
- Applies previous tag to cluster
- Validates health check again
- Comments on PR with rollback details

**Duration:** ~12-15 minutes

**Post-Deployment Notification:**

- Comment on PR with final status
- Links to production environment
- Deployment metrics

---

#### 9. **notify** - Pipeline Summary

```yaml
name: Pipeline Notification
runs-on: ubuntu-latest
needs: [build, test, quality-check, docker-build]
if: always()
```

**Purpose:** Final status summary

**Output:**

```
==========================================
✅ SUCCESS / ❌ FAILED
==========================================
Build: PASSED/FAILED
Tests: PASSED/FAILED
Quality: PASSED/FAILED
Docker: SKIPPED/PASSED/FAILED
==========================================
```

---

### Environment Variables

```bash
REGISTRY=docker.io
IMAGE_NAME=${DOCKER_HUB_USERNAME}/billing-service
JAVA_VERSION=21
MAVEN_VERSION=3.9.9
```

### GitHub Secrets Required

| Secret                | Used In       | Example                   |
| --------------------- | ------------- | ------------------------- |
| `DOCKER_HUB_USERNAME` | docker-build  | myusername                |
| `DOCKER_HUB_TOKEN`    | docker-build  | dckr_pat_xxxx             |
| `SONAR_TOKEN`         | quality-check | squ_xxxx                  |
| `SONAR_ORGANIZATION`  | quality-check | my-org                    |
| `SONAR_HOST_URL`      | quality-check | https://sonarcloud.io     |
| `AWS_ROLE_TO_ASSUME`  | deploy-\*     | arn:aws:iam::xxx:role/xxx |
| `AWS_REGION`          | deploy-\*     | us-east-1                 |
| `TF_BACKEND_BUCKET`   | deploy-\*     | billing-tfstate           |
| `KUBECONFIG`          | deploy-\*     | base64:xxx                |

---

### Conditional Execution

Jobs execute based on these conditions:

```yaml
# Always runs
build, test, quality-check, notify

# Only on main branch push
docker-build if: github.ref == 'refs/heads/main'
security-scan if: github.ref == 'refs/heads/main'
deploy-dev if: github.ref == 'refs/heads/main'

# Only after successful dev
deploy-homolog needs: deploy-dev

# Only after successful homolog + manual approval
deploy-prod needs: deploy-homolog
```

---

### Error Handling

| Stage          | Error             | Action                          |
| -------------- | ----------------- | ------------------------------- |
| build          | Maven error       | Fail pipeline, show logs        |
| test           | Test failure      | Fail pipeline, publish reports  |
| quality-check  | QG failure        | Fail pipeline, show violations  |
| docker-build   | Build failure     | Fail pipeline, check Dockerfile |
| security-scan  | CVE found         | Log scan results, continue      |
| deploy-dev     | Terraform error   | Fail pipeline, check state      |
| deploy-homolog | Health check fail | Fail with warning               |
| deploy-prod    | Health check fail | **AUTOMATIC ROLLBACK**          |

---

### Artifact Retention

| Artifact         | Retention | Purpose                 |
| ---------------- | --------- | ----------------------- |
| application-jar  | 1 day     | Build validation only   |
| test-reports     | 30 days   | Audit trail, compliance |
| coverage reports | 30 days   | Trend analysis          |

---

### Performance Optimization

**Caching:**

- Maven dependencies cached via actions/setup-java
- Docker layers cached via BuildKit
- GitHub Actions cache for build artifacts

**Parallelization:**

- `build` runs alone
- `test` and `quality-check` run in parallel after build
- `docker-build` and `security-scan` run in parallel
- `deploy-dev` waits for all upstream jobs
- `deploy-homolog` waits for `deploy-dev` (5 min buffer)
- `deploy-prod` waits for `deploy-homolog`

**Timeouts:**

- Built-in step timeouts prevent hanging
- Job-level timeout configurable per environment
- Health checks use loops with max attempts

---

## Debugging

### View Full Logs

```bash
gh workflow run ci-cd.yml -r main
gh run list --workflow ci-cd.yml
gh run view <run-id>
gh run view <run-id> --log
```

### Re-run Failed Jobs

```bash
gh run rerun <run-id>
gh run rerun <run-id> --failed
```

### Artifacts

```bash
gh run download <run-id> -n test-reports
gh run download <run-id> -n application-jar
```

---

## Next Steps

1. **[CI_CD_SETUP.md] - Complete Setup Guide** ← Start here
2. **[PIPELINE.md] - Pipeline Overview** ← Understanding the flow
3. **[.github/workflows/ci-cd.yml] - Source Code** ← Deep dive

---

**Last Updated:** February 17, 2026
