#!/bin/bash

################################################################################
# Billing Service - Unified Startup & Configuration Script
# 
# This script provides a centralized interface for:
# - Starting/stopping services (full stack, DB only, with PgAdmin)
# - Managing Docker containers and volumes
# - Setting up AWS SQS queues with Dead Letter Queue for local development
# - Health checks and diagnostics
# - Viewing logs and accessing services
#
# Usage: ./start.sh [command] [options]
################################################################################

set -e

# Colors for output
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly RED='\033[0;31m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m' # No Color

# Configuration
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_NAME="billing-service"
readonly ENDPOINT="http://localhost:4566"
readonly REGION="us-east-2"
readonly MAIN_QUEUE="payment-request-queue"
readonly DLQ="payment-request-dlq"
readonly SUCCESS_QUEUE="payment-response-success-queue"
readonly FAILURE_QUEUE="payment-response-failure-queue"

################################################################################
# Helper Functions
################################################################################

print_header() {
    echo ""
    echo -e "${BLUE}======================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}======================================${NC}"
    echo ""
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

# Check prerequisites
check_prerequisites() {
    print_info "Checking prerequisites..."
    
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker first."
        exit 1
    fi
    print_success "Docker is installed"
    
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose is not installed. Please install Docker Compose first."
        exit 1
    fi
    print_success "Docker Compose is installed"
    
    if ! command -v aws &> /dev/null; then
        print_warning "AWS CLI is not installed. Some features will be unavailable."
    else
        print_success "AWS CLI is installed"
    fi
}

# Check and setup .env file
setup_env_file() {
    if [ ! -f .env ]; then
        print_warning ".env file not found. Creating from .env.example..."
        if [ ! -f .env.example ]; then
            print_error ".env.example not found. Cannot create .env"
            exit 1
        fi
        cp .env.example .env
        print_warning "Default .env created. Please edit with your actual credentials:"
        echo ""
        echo "Required variables:"
        echo "  - AWS_ACCESS_KEY"
        echo "  - AWS_SECRET_KEY"
        echo "  - MERCADOPAGO_ACCESS_TOKEN"
        echo "  - MERCADOPAGO_PUBLIC_KEY"
        echo ""
        read -p "Press Enter after you've configured .env file..."
    fi
    print_success ".env file is configured"
}

################################################################################
# Docker Compose Operations
################################################################################

start_full_stack() {
    print_header "$PROJECT_NAME - Starting Full Stack"
    setup_env_file
    
    print_info "Building and starting services..."
    docker-compose up -d --build
    
    print_success "Full stack started!"
    echo ""
    echo "Access points:"
    echo "  • Application:    http://localhost:8080"
    echo "  • Health Check:   http://localhost:8080/actuator/health"
    echo "  • Database:       localhost:5433 (postgres/postgres123)"
    echo "  • Flyway Schema:  Auto-migrated on startup"
    echo ""
    print_info "View logs with: docker-compose logs -f"
    print_info "Stop services with: ./start.sh stop"
}

start_database_only() {
    print_header "$PROJECT_NAME - Database Only"
    setup_env_file
    
    print_info "Starting PostgreSQL database (development profile)..."
    docker-compose -f docker-compose.dev.yml up -d
    
    print_success "Database started!"
    echo ""
    echo "PostgreSQL is available at:"
    echo "  • Host:     localhost"
    echo "  • Port:     5433"
    echo "  • Database: billing_db"
    echo "  • User:     postgres"
    echo "  • Password: postgres123"
    echo ""
    echo "Run the application locally with:"
    echo "  export SPRING_PROFILES_ACTIVE=development"
    echo "  mvn spring-boot:run"
}

start_with_pgadmin() {
    print_header "$PROJECT_NAME - With PgAdmin"
    setup_env_file
    
    print_info "Starting services with PgAdmin..."
    docker-compose --profile dev up -d --build
    
    print_success "Services started with PgAdmin!"
    echo ""
    echo "Access points:"
    echo "  • Application:    http://localhost:8080"
    echo "  • PgAdmin:        http://localhost:5050"
    echo "    - Email:        admin@billing-service.com"
    echo "    - Password:     admin123"
    echo "  • Database:       localhost:5433"
    echo ""
    print_info "View logs with: docker-compose logs -f"
}

stop_services() {
    print_header "$PROJECT_NAME - Stopping Services"
    
    print_info "Stopping full stack..."
    docker-compose down || true
    
    print_info "Stopping database..."
    docker-compose -f docker-compose.dev.yml down || true
    
    print_success "All services stopped!"
}

cleanup_everything() {
    print_header "$PROJECT_NAME - Cleanup"
    
    echo -e "${RED}This action will:${NC}"
    echo "  • Remove all containers"
    echo "  • Delete all volumes (DATABASE DATA WILL BE LOST)"
    echo "  • Remove images"
    echo ""
    
    read -p "Are you sure? Type 'yes' to continue: " confirm
    
    if [ "$confirm" != "yes" ]; then
        print_info "Cleanup cancelled"
        return
    fi
    
    print_warning "Removing containers and volumes..."
    docker-compose down -v || true
    docker-compose -f docker-compose.dev.yml down -v || true
    
    print_success "Cleanup complete!"
    print_info "All data has been removed. Safe to delete and reinstall."
}

show_logs() {
    local service="${1:-}"
    print_header "$PROJECT_NAME - Logs"
    
    if [ -z "$service" ]; then
        print_info "Showing logs from all services (Ctrl+C to exit)..."
        docker-compose logs -f
    else
        print_info "Showing logs from $service (Ctrl+C to exit)..."
        docker-compose logs -f "$service"
    fi
}

show_status() {
    print_header "$PROJECT_NAME - Status"
    
    print_info "Container status:"
    docker-compose ps
    
    echo ""
    if command -v docker-stats &> /dev/null; then
        print_info "Resource usage:"
        docker stats --no-stream 2>/dev/null || true
    fi
}

health_check() {
    print_header "$PROJECT_NAME - Health Check"
    
    print_info "Checking application health..."
    if curl -s http://localhost:8080/actuator/health | grep -q '"status":"UP"'; then
        print_success "Application is healthy"
    else
        print_warning "Application is not responding on port 8080"
    fi
    
    print_info "Checking database connectivity..."
    if docker-compose exec -T postgres pg_isready -U postgres &> /dev/null; then
        print_success "Database is healthy"
    else
        print_warning "Database is not responding"
    fi
}

################################################################################
# AWS SQS Queue Management
################################################################################

setup_sqs_queues() {
    print_header "$PROJECT_NAME - Setup SQS Queues with DLQ"
    
    if ! command -v aws &> /dev/null; then
        print_error "AWS CLI is not installed. Cannot setup SQS queues."
        echo ""
        print_info "Install AWS CLI with: pip install awscli"
        return 1
    fi
    
    print_info "Checking LocalStack availability..."
    if ! curl -s $ENDPOINT/health &> /dev/null; then
        print_warning "LocalStack is not running. Start with full stack first: ./start.sh up"
        return 1
    fi
    print_success "LocalStack is available"
    
    print_info "Creating Dead Letter Queue: $DLQ"
    DLQ_URL=$(aws sqs create-queue \
        --endpoint-url $ENDPOINT \
        --region $REGION \
        --queue-name $DLQ \
        --query 'QueueUrl' \
        --output text 2>/dev/null || aws sqs get-queue-url \
        --endpoint-url $ENDPOINT \
        --region $REGION \
        --queue-name $DLQ \
        --query 'QueueUrl' \
        --output text)
    
    print_success "DLQ created: $DLQ_URL"
    
    # Get DLQ ARN
    print_info "Retrieving DLQ ARN..."
    DLQ_ARN=$(aws sqs get-queue-attributes \
        --endpoint-url $ENDPOINT \
        --region $REGION \
        --queue-url $DLQ_URL \
        --attribute-names QueueArn \
        --query 'Attributes.QueueArn' \
        --output text)
    
    print_success "DLQ ARN: $DLQ_ARN"
    
    # Create redrive policy
    print_info "Creating redrive policy..."
    REDRIVE_POLICY=$(cat <<EOF
{
  "deadLetterTargetArn": "$DLQ_ARN",
  "maxReceiveCount": "3"
}
EOF
)
    
    # Create main queue
    print_info "Creating main queue: $MAIN_QUEUE"
    MAIN_QUEUE_URL=$(aws sqs create-queue \
        --endpoint-url $ENDPOINT \
        --region $REGION \
        --queue-name $MAIN_QUEUE \
        --attributes "{\"RedrivePolicy\":\"$(echo $REDRIVE_POLICY | sed 's/"/\\"/g')\",\"VisibilityTimeout\":\"300\"}" \
        --query 'QueueUrl' \
        --output text 2>/dev/null || aws sqs get-queue-url \
        --endpoint-url $ENDPOINT \
        --region $REGION \
        --queue-name $MAIN_QUEUE \
        --query 'QueueUrl' \
        --output text)
    
    print_success "Main queue created: $MAIN_QUEUE_URL"
    
    # Create response queues
    print_info "Creating response queues..."
    
    SUCCESS_QUEUE_URL=$(aws sqs create-queue \
        --endpoint-url $ENDPOINT \
        --region $REGION \
        --queue-name $SUCCESS_QUEUE \
        --query 'QueueUrl' \
        --output text 2>/dev/null || aws sqs get-queue-url \
        --endpoint-url $ENDPOINT \
        --region $REGION \
        --queue-name $SUCCESS_QUEUE \
        --query 'QueueUrl' \
        --output text)
    
    FAILURE_QUEUE_URL=$(aws sqs create-queue \
        --endpoint-url $ENDPOINT \
        --region $REGION \
        --queue-name $FAILURE_QUEUE \
        --query 'QueueUrl' \
        --output text 2>/dev/null || aws sqs get-queue-url \
        --endpoint-url $ENDPOINT \
        --region $REGION \
        --queue-name $FAILURE_QUEUE \
        --query 'QueueUrl' \
        --output text)
    
    print_success "Response queues created"
    
    # Display configuration
    echo ""
    print_success "SQS Queues configured successfully!"
    echo ""
    echo "Configuration Summary:"
    echo "  Main Queue:              $MAIN_QUEUE"
    echo "  Dead Letter Queue:       $DLQ"
    echo "  Success Response Queue:  $SUCCESS_QUEUE"
    echo "  Failure Response Queue:  $FAILURE_QUEUE"
    echo ""
    echo "  Max Receive Count:       3"
    echo "  Visibility Timeout:      300 seconds (5 minutes)"
    echo ""
    echo "Behavior:"
    echo "  • Messages failing 3 times will move to DLQ"
    echo "  • Visibility timeout allows 5 minutes for processing"
    echo "  • Idempotency prevents duplicate payment creation"
    echo ""
    
    return 0
}

delete_sqs_queues() {
    print_header "$PROJECT_NAME - Delete SQS Queues"
    
    if ! command -v aws &> /dev/null; then
        print_error "AWS CLI is not installed. Cannot delete SQS queues."
        return 1
    fi
    
    echo -e "${RED}This will delete all SQS queues:${NC}"
    echo "  • $MAIN_QUEUE"
    echo "  • $DLQ"
    echo "  • $SUCCESS_QUEUE"
    echo "  • $FAILURE_QUEUE"
    echo ""
    
    read -p "Are you sure? Type 'yes' to continue: " confirm
    
    if [ "$confirm" != "yes" ]; then
        print_info "Deletion cancelled"
        return
    fi
    
    for queue in $MAIN_QUEUE $DLQ $SUCCESS_QUEUE $FAILURE_QUEUE; do
        print_info "Deleting queue: $queue"
        QUEUE_URL=$(aws sqs get-queue-url \
            --endpoint-url $ENDPOINT \
            --region $REGION \
            --queue-name $queue \
            --query 'QueueUrl' \
            --output text 2>/dev/null || echo "")
        
        if [ -n "$QUEUE_URL" ]; then
            aws sqs delete-queue \
                --endpoint-url $ENDPOINT \
                --queue-url $QUEUE_URL
            print_success "Deleted: $queue"
        else
            print_warning "Queue not found: $queue"
        fi
    done
    
    echo ""
    print_success "All queues deleted!"
}

################################################################################
# Help and Usage
################################################################################

show_help() {
    cat << 'EOF'
╔════════════════════════════════════════════════════════════════════════════╗
║                  Billing Service - Unified Control Script                  ║
╚════════════════════════════════════════════════════════════════════════════╝

USAGE:
  ./start.sh [COMMAND] [OPTIONS]

COMMANDS:

  DOCKER & SERVICES:
    up, start             Start full stack (Application + PostgreSQL)
    db                    Start database only (for local development)
    pgadmin               Start with PgAdmin (database management UI)
    stop, down            Stop all services
    restart               Restart all services
    clean, cleanup        Remove all containers and volumes (⚠️ DATA LOSS)
    
  MONITORING:
    logs [SERVICE]        View logs (all services or specific: 'app', 'postgres')
    status                Show container status and resource usage
    health                Health check for application and database
    
  SQS QUEUE MANAGEMENT:
    setup-queues          Setup SQS queues with Dead Letter Queue (LocalStack)
    delete-queues         Delete all SQS queues
    
  UTILITIES:
    help                  Show this help message
    version               Show script version
    

EXAMPLES:

  # Start everything
  ./start.sh up

  # Start just the database
  ./start.sh db

  # View application logs
  ./start.sh logs app

  # Setup SQS queues for development
  ./start.sh setup-queues

  # Show status
  ./start.sh status


SERVICES & ENDPOINTS:

  Application:    http://localhost:8080
  Health Check:   http://localhost:8080/actuator/health
  PgAdmin:        http://localhost:5050 (with --pgadmin flag)
  Database:       localhost:5433 (postgres / postgres123)


ENVIRONMENT:

  Configuration is loaded from .env file.
  Copy .env.example to .env and configure with your credentials:
  
    AWS_REGION=us-east-2
    AWS_ACCESS_KEY=your-key
    AWS_SECRET_KEY=your-secret
    MERCADOPAGO_ACCESS_TOKEN=your-token
    MERCADOPAGO_PUBLIC_KEY=your-public-key


TROUBLESHOOTING:

  Port already in use?
    lsof -i :8080
    kill -9 <PID>

  Check logs:
    docker-compose logs -f app
    docker-compose logs -f postgres

  Database issues:
    docker-compose exec postgres psql -U postgres -d billing_db

  Reset everything:
    ./start.sh clean


For more details, see README.md

EOF
}

################################################################################
# Main Menu (Interactive Mode)
################################################################################

show_interactive_menu() {
    print_header "Billing Service - Quick Start Menu"
    
    echo "Choose an option:"
    echo ""
    echo "DOCKER & SERVICES:"
    echo "  1) Start full stack (Application + PostgreSQL)"
    echo "  2) Start database only (for local development)"
    echo "  3) Start with PgAdmin (for database management)"
    echo "  4) Stop all services"
    echo "  5) Restart all services"
    echo ""
    echo "MONITORING:"
    echo "  6) View logs"
    echo "  7) Show status"
    echo "  8) Health check"
    echo ""
    echo "AWS SQS:"
    echo "  9) Setup SQS queues with DLQ"
    echo "  10) Delete SQS queues"
    echo ""
    echo "UTILITIES:"
    echo "  11) Clean everything (remove containers & volumes)"
    echo "  12) Show help"
    echo "  0) Exit"
    echo ""
    
    read -p "Enter your choice [0-12]: " choice
    
    case $choice in
        1)  start_full_stack ;;
        2)  start_database_only ;;
        3)  start_with_pgadmin ;;
        4)  stop_services ;;
        5)  stop_services && start_full_stack ;;
        6)  show_logs ;;
        7)  show_status ;;
        8)  health_check ;;
        9)  setup_sqs_queues ;;
        10) delete_sqs_queues ;;
        11) cleanup_everything ;;
        12) show_help ;;
        0)  print_info "Goodbye!"; exit 0 ;;
        *)  print_error "Invalid choice"; show_interactive_menu ;;
    esac
}

################################################################################
# Main Entry Point
################################################################################

main() {
    # Check prerequisites first
    check_prerequisites
    
    # If no arguments provided, show interactive menu
    if [ $# -eq 0 ]; then
        show_interactive_menu
        return $?
    fi
    
    # Handle command-line arguments
    local command="$1"
    shift
    
    case "$command" in
        up|start)
            start_full_stack
            ;;
        db)
            start_database_only
            ;;
        pgadmin|dev)
            start_with_pgadmin
            ;;
        stop|down)
            stop_services
            ;;
        restart)
            stop_services
            sleep 2
            start_full_stack
            ;;
        logs)
            show_logs "$@"
            ;;
        status)
            show_status
            ;;
        health)
            health_check
            ;;
        setup-queues|setup-sqs)
            setup_sqs_queues
            ;;
        delete-queues|delete-sqs)
            delete_sqs_queues
            ;;
        clean|cleanup)
            cleanup_everything
            ;;
        help|--help|-h)
            show_help
            ;;
        version|--version|-v)
            echo "Billing Service Startup Script v2.0.0"
            ;;
        *)
            print_error "Unknown command: $command"
            echo ""
            show_help
            exit 1
            ;;
    esac
}

# Run main function
main "$@"

# Menu
echo ""
echo "Choose an option:"
echo "1) Start full stack (Application + PostgreSQL)"
echo "2) Start database only (for local development)"
echo "3) Start with PgAdmin (for database management)"
echo "4) Stop all services"
echo "5) View logs"
echo "6) Clean up (remove containers and volumes)"
echo ""
read -p "Enter your choice [1-6]: " choice

case $choice in
    1)
        echo ""
        echo "Starting full stack..."
        docker-compose up -d --build
        print_success "Services started!"
        echo ""
        echo "Access points:"
        echo "  - Application: http://localhost:8080"
        echo "  - Health Check: http://localhost:8080/actuator/health"
        echo "  - PostgreSQL: localhost:5433"
        echo ""
        echo "View logs with: docker-compose logs -f"
        ;;
    2)
        echo ""
        echo "Starting database only..."
        docker-compose -f docker-compose.dev.yml up -d
        print_success "Database started!"
        echo ""
        echo "PostgreSQL is available at: localhost:5433"
        echo "  - Username: postgres"
        echo "  - Password: postgres123"
        echo "  - Database: billing_db"
        echo ""
        echo "Now run the application locally with:"
        echo "  mvn spring-boot:run"
        ;;
    3)
        echo ""
        echo "Starting with PgAdmin..."
        docker-compose --profile dev up -d --build
        print_success "Services started with PgAdmin!"
        echo ""
        echo "Access points:"
        echo "  - Application: http://localhost:8080"
        echo "  - PgAdmin: http://localhost:5050"
        echo "    Email: admin@billing-service.com"
        echo "    Password: admin123"
        echo "  - PostgreSQL: localhost:5433"
        ;;
    4)
        echo ""
        echo "Stopping all services..."
        docker-compose down
        docker-compose -f docker-compose.dev.yml down
        print_success "All services stopped!"
        ;;
    5)
        echo ""
        echo "Showing logs (Ctrl+C to exit)..."
        docker-compose logs -f
        ;;
    6)
        echo ""
        read -p "This will remove all containers and data. Continue? (y/N): " confirm
        if [ "$confirm" = "y" ] || [ "$confirm" = "Y" ]; then
            docker-compose down -v
            docker-compose -f docker-compose.dev.yml down -v
            print_success "Cleanup complete!"
        else
            echo "Cleanup cancelled"
        fi
        ;;
    *)
        print_error "Invalid choice"
        exit 1
        ;;
esac

echo ""
print_success "Done!"
