#!/bin/bash

# Quick Start Script for Billing Service
# This script helps you get started with the Billing Service quickly

set -e

echo "======================================"
echo "Billing Service - Quick Start"
echo "======================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to print colored messages
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    print_error "Docker is not installed. Please install Docker first."
    exit 1
fi

print_success "Docker is installed"

# Check if Docker Compose is installed
if ! command -v docker-compose &> /dev/null; then
    print_error "Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

print_success "Docker Compose is installed"

# Check if .env file exists
if [ ! -f .env ]; then
    print_warning ".env file not found. Creating from .env.example..."
    cp .env.example .env
    print_warning "Please edit .env file with your actual credentials before proceeding."
    echo ""
    echo "Required variables:"
    echo "  - AWS_ACCESS_KEY"
    echo "  - AWS_SECRET_KEY"
    echo "  - MERCADOPAGO_ACCESS_TOKEN"
    echo ""
    read -p "Press Enter after you've configured .env file..."
fi

print_success ".env file exists"

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
