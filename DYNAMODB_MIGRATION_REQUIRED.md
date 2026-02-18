# 🔄 Payment Service - Migração para DynamoDB

## ⚠️ Ação Necessária: Atualizar Código Java

A infraestrutura Terraform foi atualizada para usar **DynamoDB** ao invés de **PostgreSQL RDS**.

**Status:**
- ✅ **Terraform**: Atualizado e pronto para deploy
- ⏳ **Código Java**: Requer atualização manual

---

## 📋 Alterações de Terraform Já Realizadas

### ✅ Arquivos Modificados

1. **terraform/providers.tf**
   - Remote state: `rds_billing` → `dynamodb_billing`

2. **terraform/configmap.tf**
   - Removido: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`
   - Adicionado: `AWS_DYNAMODB_TABLE_NAME`

3. **terraform/secret.tf**
   - Removido: `SPRING_DATASOURCE_PASSWORD`

4. **terraform/variables.tf**
   - Removido: variável `db_password`

5. **terraform/secret.tfvars**
   - Removido: valor `db_password`

---

## 🔧 Alterações Necessárias no Código Java

### 1. Dependências (pom.xml)

#### ❌ REMOVER (PostgreSQL/JPA):

```xml
<!-- PostgreSQL -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>

<!-- Spring Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Flyway Migrations -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

#### ✅ ADICIONAR (DynamoDB):

```xml
<!-- AWS SDK v2 - DynamoDB Enhanced Client -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>dynamodb-enhanced</artifactId>
    <version>2.20.26</version>
</dependency>

<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>dynamodb</artifactId>
    <version>2.20.26</version>
</dependency>
```

---

### 2. Entity - Payment.java

#### ❌ ANTES (JPA):

```java
import javax.persistence.*;

@Entity
@Table(name = "payments")
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(name = "order_id", nullable = false)
    private String orderId;
    
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // ... getters e setters
}
```

#### ✅ DEPOIS (DynamoDB):

```java
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import java.time.Instant;

@DynamoDbBean
public class Payment {
    
    private String paymentId;      // UUID
    private String createdAt;      // ISO-8601 timestamp (Sort Key)
    private String orderId;
    private BigDecimal amount;
    private String currency;
    private String status;         // PENDING, APPROVED, FAILED
    private String paymentMethod;
    private String merchantOrderId;
    private String externalId;
    private Map<String, String> metadata;
    
    @DynamoDbPartitionKey
    @DynamoDbAttribute("paymentId")
    public String getPaymentId() {
        return paymentId;
    }
    
    @DynamoDbSortKey
    @DynamoDbAttribute("createdAt")
    public String getCreatedAt() {
        return createdAt;
    }
    
    @DynamoDbSecondaryPartitionKey(indexNames = "OrderIdIndex")
    @DynamoDbAttribute("orderId")
    public String getOrderId() {
        return orderId;
    }
    
    @DynamoDbSecondaryPartitionKey(indexNames = "StatusIndex")
    @DynamoDbAttribute("status")
    public String getStatus() {
        return status;
    }
    
    // ... outros getters e setters
    
    @PrePersist
    public void prePersist() {
        if (paymentId == null) {
            paymentId = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = Instant.now().toString();
        }
    }
}
```

---

### 3. Repository

#### ❌ REMOVER (JPA Repository):

```java
@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findByOrderId(String orderId);
    List<Payment> findByStatus(PaymentStatus status);
}
```

#### ✅ CRIAR (DynamoDB Repository):

```java
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;

@Repository
public class PaymentRepository {
    
    private final DynamoDbTable<Payment> paymentTable;
    
    public PaymentRepository(
            DynamoDbEnhancedClient enhancedClient,
            @Value("${aws.dynamodb.table-name}") String tableName) {
        
        this.paymentTable = enhancedClient.table(
            tableName, 
            TableSchema.fromBean(Payment.class)
        );
    }
    
    /**
     * Salvar/Atualizar pagamento
     */
    public Payment save(Payment payment) {
        payment.prePersist();  // Garantir IDs
        paymentTable.putItem(payment);
        return payment;
    }
    
    /**
     * Buscar por paymentId (PK) e createdAt (SK)
     */
    public Optional<Payment> findById(String paymentId, String createdAt) {
        Key key = Key.builder()
            .partitionValue(paymentId)
            .sortValue(createdAt)
            .build();
        
        return Optional.ofNullable(paymentTable.getItem(key));
    }
    
    /**
     * Buscar por orderId usando GSI OrderIdIndex
     */
    public List<Payment> findByOrderId(String orderId) {
        QueryConditional queryConditional = QueryConditional
            .keyEqualTo(Key.builder()
                .partitionValue(orderId)
                .build());
        
        return paymentTable
            .index("OrderIdIndex")
            .query(queryConditional)
            .items()
            .stream()
            .toList();
    }
    
    /**
     * Buscar por status usando GSI StatusIndex
     */
    public List<Payment> findByStatus(String status) {
        QueryConditional queryConditional = QueryConditional
            .keyEqualTo(Key.builder()
                .partitionValue(status)
                .build());
        
        return paymentTable
            .index("StatusIndex")
            .query(queryConditional)
            .items()
            .stream()
            .toList();
    }
    
    /**
     * Deletar pagamento
     */
    public void delete(String paymentId, String createdAt) {
        Key key = Key.builder()
            .partitionValue(paymentId)
            .sortValue(createdAt)
            .build();
        
        paymentTable.deleteItem(key);
    }
}
```

---

### 4. Configuration - DynamoDbConfig.java

#### ✅ CRIAR:

```java
package com.fiap.billing_service.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
public class DynamoDbConfig {
    
    @Value("${aws.region:us-east-2}")
    private String awsRegion;
    
    @Value("${aws.access-key-id:#{null}}")
    private String awsAccessKey;
    
    @Value("${aws.secret-access-key:#{null}}")
    private String awsSecretKey;
    
    @Bean
    public DynamoDbClient dynamoDbClient() {
        var builder = DynamoDbClient.builder()
            .region(Region.of(awsRegion));
        
        // Se credenciais fornecidas (dev/homolog), usa-as
        // Em produção, usar IAM Roles (IRSA)
        if (awsAccessKey != null && !awsAccessKey.isEmpty() 
            && awsSecretKey != null && !awsSecretKey.isEmpty()) {
            
            builder.credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(awsAccessKey, awsSecretKey)
                )
            );
        } else {
            // Fallback para default credentials chain (IAM Role, env vars, etc)
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        
        return builder.build();
    }
    
    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
            .dynamoDbClient(dynamoDbClient)
            .build();
    }
}
```

---

### 5. Application Properties

#### ❌ REMOVER:

```properties
# PostgreSQL
spring.datasource.url=${SPRING_DATASOURCE_URL}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA
spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

#### ✅ ADICIONAR:

```properties
# DynamoDB
aws.dynamodb.table-name=${AWS_DYNAMODB_TABLE_NAME:challengeone-billing-dev}

# AWS Credentials (opcional - fallback para IRSA)
aws.access-key-id=${AWS_ACCESS_KEY:}
aws.secret-access-key=${AWS_SECRET_KEY:}
aws.region=${AWS_REGION:us-east-2}
```

---

### 6. Service Layer (Se Necessário)

Se você tinha código no service que dependia de funcionalidades JPA específicas:

#### ❌ ANTES:
```java
// Queries derivadas automáticas
List<Payment> payments = paymentRepository.findByStatus(PaymentStatus.PENDING);

// Specification/Criteria API
Specification<Payment> spec = ...;
List<Payment> results = paymentRepository.findAll(spec);
```

#### ✅ DEPOIS:
```java
// Usar métodos implementados no repository
List<Payment> payments = paymentRepository.findByStatus("PENDING");

// Queries complexas: Filtrar em memória ou criar GSI adicional
```

---

## 🚀 Passos para Deploy

### 1. Deploy Tabela DynamoDB (PRIMEIRO)

```powershell
cd e:\code\dynamodb-billing\envs\dev
.\deploy.ps1
```

### 2. Atualizar Código Java

- [ ] Atualizar pom.xml (dependências)
- [ ] Atualizar Payment entity (anotações DynamoDB)
- [ ] Criar PaymentRepository (DynamoDB)
- [ ] Criar DynamoDbConfig
- [ ] Atualizar application.properties
- [ ] Adaptar services se necessário
- [ ] Executar testes locais

### 3. Build Nova Imagem Docker

```powershell
cd e:\code\payment-service

# Build
docker build -t thiagotierre/billing-service:latest .

# Push
docker push thiagotierre/billing-service:latest
```

### 4. Deploy Kubernetes (DEPOIS)

```powershell
cd e:\code\payment-service\terraform

# Reinicializar terraform (remote state mudou)
terraform init -reconfigure

# Aplicar mudanças
terraform apply -var-file=terraform.tfvars -var-file=secret.tfvars
```

### 5. Validar

```powershell
# Verificar pods
kubectl get pods -n challengeone-billing

# Logs
kubectl logs -n challengeone-billing -l app=billing --tail=100 -f

# Testar endpoint
kubectl port-forward -n challengeone-billing svc/billing 8080:8080
curl http://localhost:8080/actuator/health
```

---

## 📚 Recursos

- [CHANGES_SUMMARY.md](../dynamodb-billing/CHANGES_SUMMARY.md) - Resumo completo
- [MIGRATION_GUIDE.md](../dynamodb-billing/MIGRATION_GUIDE.md) - Guia detalhado
- [dynamodb-billing README](../dynamodb-billing/README.md) - Infraestrutura

### Documentação Oficial

- [AWS SDK v2 - DynamoDB Enhanced Client](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/dynamodb-enhanced-client.html)
- [DynamoDB Best Practices](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/best-practices.html)
- [Spring Boot with DynamoDB](https://reflectoring.io/spring-dynamodb/)

---

## 📊 Comparação

| Aspecto | PostgreSQL | DynamoDB |
|---------|-----------|----------|
| **Dependências** | spring-data-jpa, postgresql, flyway | aws-sdk dynamodb-enhanced |
| **Entity** | `@Entity`, `@Table`, `@Id` | `@DynamoDbBean`, `@DynamoDbPartitionKey` |
| **Repository** | `extends JpaRepository` | Classe manual com DynamoDbTable |
| **Queries** | JPQL/SQL | Key-based + GSI |
| **Migrations** | Flyway/Liquibase | Schema-less (não precisa) |

---

## ⚠️ Avisos Importantes

1. **Queries Complexas**: DynamoDB não suporta JOINs nem queries SQL avançadas
2. **GSI Limits**: Máximo 20 GSIs por tabela
3. **Item Size**: Máximo 400KB por item
4. **Transactions**: Limitadas a 25 items por transação
5. **Custos**: Pay-per-request cobra por read/write (estime custos antes)

---

**Status:** ⏳ Aguardando atualização do código Java  
**Data:** 2026-02-17
