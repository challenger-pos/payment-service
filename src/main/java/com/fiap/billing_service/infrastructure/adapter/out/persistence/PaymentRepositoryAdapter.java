package com.fiap.billing_service.infrastructure.adapter.out.persistence;

import com.fiap.billing_service.application.port.out.PaymentRepositoryPort;
import com.fiap.billing_service.domain.entity.Payment;
import com.fiap.billing_service.infrastructure.adapter.out.persistence.entity.PaymentEntity;
import com.fiap.billing_service.infrastructure.adapter.out.persistence.mapper.PaymentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter for Payment persistence using Amazon DynamoDB.
 * 
 * This adapter implements the PaymentRepositoryPort and provides CRUD operations
 * for Payment entities stored in DynamoDB. It uses the Enhanced Client from AWS SDK v2
 * which simplifies serialization/deserialization of DynamoDB items.
 * 
 * Table Design:
 * - Partition Key: workOrderId (ensures uniqueness and distribution)
 * - Sort Key: createdAt (enables temporal range queries)
 * - Global Secondary Indexes: Support queries by clientId, status, and externalPaymentId
 */
@Component
public class PaymentRepositoryAdapter implements PaymentRepositoryPort {

  private static final Logger logger = LoggerFactory.getLogger(PaymentRepositoryAdapter.class);

  @Value("${dynamodb.table-name:payments}")
  private String tableName;

  private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
  private final PaymentMapper mapper;

  public PaymentRepositoryAdapter(DynamoDbEnhancedClient dynamoDbEnhancedClient, 
                                   PaymentMapper mapper) {
    this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
    this.mapper = mapper;
  }

  /**
   * Get the DynamoDB table reference for Payment entities.
   * 
   * @return DynamoDB table reference
   */
  private DynamoDbTable<PaymentEntity> getPaymentTable() {
    return dynamoDbEnhancedClient.table(tableName, TableSchema.fromBean(PaymentEntity.class));
  }

  /**
   * Save a payment to DynamoDB.
   * 
   * This method converts the domain Payment entity to PaymentEntity and stores it
   * in DynamoDB. It performs a PutItem operation.
   * 
   * @param payment Domain entity to save
   * @return The saved payment (domain entity)
   * @throws DynamoDbException if the save operation fails
   */
  @Override
  public Payment save(Payment payment) {
    try {
      PaymentEntity entity = mapper.toEntity(payment);
      
      // Log the operation for debugging
      logger.debug("Saving payment with workOrderId: {} to DynamoDB table: {}", 
                   payment.getWorkOrderId(), tableName);
      
      DynamoDbTable<PaymentEntity> table = getPaymentTable();
      table.putItem(entity);
      
      logger.debug("Payment saved successfully with workOrderId: {}", payment.getWorkOrderId());
      return payment;
    } catch (DynamoDbException e) {
      logger.error("Error saving payment with workOrderId: {} - Error: {}", 
                   payment.getWorkOrderId(), e.getMessage(), e);
      throw e;
    }
  }

  /**
   * Find a payment by work order ID.
   * 
   * Since workOrderId is the partition key of the DynamoDB table,
   * this is a direct key lookup which is very efficient.
   * 
   * Note: The sort key (createdAt) is not needed for this lookup since
   * workOrderId is unique per the business requirements.
   * 
   * @param workOrderId The work order ID (partition key)
   * @return Optional containing the payment if found, empty otherwise
   * @throws DynamoDbException if the query fails
   */
  @Override
  public Optional<Payment> findByWorkOrderId(UUID workOrderId) {
    try {
      logger.debug("Finding payment with workOrderId: {} from DynamoDB table: {}", 
                   workOrderId, tableName);
      
      DynamoDbTable<PaymentEntity> table = getPaymentTable();
      
      // Query using partition key. UUID must be converted to String
      PaymentEntity entity = table.getItem(r -> r.key(k -> k
          .partitionValue(workOrderId.toString())
      ));
      
      if (entity != null) {
        logger.debug("Payment found with workOrderId: {}", workOrderId);
        return Optional.of(mapper.toDomain(entity));
      } else {
        logger.debug("Payment not found with workOrderId: {}", workOrderId);
        return Optional.empty();
      }
    } catch (DynamoDbException e) {
      logger.error("Error finding payment with workOrderId: {} - Error: {}",
                   workOrderId, e.getMessage(), e);
      throw e;
    }
  }

  /**
   * Update an existing payment in DynamoDB.
   * 
   * This method performs an UpdateItem operation to modify specific attributes
   * of a payment. For full replacement, use save() instead.
   * 
   * @param payment Domain entity to update
   * @return The updated payment (domain entity)
   */
  public Payment update(Payment payment) {
    try {
      logger.debug("Updating payment with workOrderId: {}", payment.getWorkOrderId());
      
      PaymentEntity entity = mapper.toEntity(payment);
      DynamoDbTable<PaymentEntity> table = getPaymentTable();
      table.updateItem(entity);
      
      logger.debug("Payment updated successfully with workOrderId: {}", payment.getWorkOrderId());
      return payment;
    } catch (DynamoDbException e) {
      logger.error("Error updating payment with workOrderId: {} - Error: {}",
                   payment.getWorkOrderId(), e.getMessage(), e);
      throw e;
    }
  }

  /**
   * Delete a payment from DynamoDB by work order ID.
   * 
   * @param workOrderId The work order ID (partition key)
   * @throws DynamoDbException if the delete operation fails
   */
  public void deleteByWorkOrderId(UUID workOrderId) {
    try {
      logger.debug("Deleting payment with workOrderId: {}", workOrderId);
      
      DynamoDbTable<PaymentEntity> table = getPaymentTable();
      table.deleteItem(r -> r.key(k -> k
          .partitionValue(workOrderId.toString())
      ));
      
      logger.debug("Payment deleted successfully with workOrderId: {}", workOrderId);
    } catch (DynamoDbException e) {
      logger.error("Error deleting payment with workOrderId: {} - Error: {}",
                   workOrderId, e.getMessage(), e);
      throw e;
    }
  }
}
