#!/bin/bash

# Script để build tất cả services
# Usage: ./build-all.sh

echo "🚀 Building Smart Retail Backend Services..."
echo ""

# Danh sách services
services=(
    "discovery-server"
    "api-gateway"
    "service-auth"
    "user-service"
    "service-customer"
    "service-product"
    "inventory-service"
    "order-service"
    "promotion-service"
    "payment-service"
)

# Build từng service
for service in "${services[@]}"; do
    echo "📦 Building $service..."
    cd "$service" || exit 1

    if [ -f "../mvnw" ]; then
        ../mvnw clean package -DskipTests
    else
        mvn clean package -DskipTests
    fi

    if [ $? -eq 0 ]; then
        echo "✅ $service built successfully"
    else
        echo "❌ Failed to build $service"
        exit 1
    fi

    cd ..
    echo ""
done

echo "🎉 All services built successfully!"
echo ""
echo "JAR files are in each service's target/ directory"

