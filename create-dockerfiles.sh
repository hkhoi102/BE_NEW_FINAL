#!/bin/bash

# Script để tạo Dockerfile cho tất cả services
# Usage: ./create-dockerfiles.sh

echo "🐳 Creating Dockerfiles for all services..."
echo ""

# Mapping service -> port
declare -A service_ports=(
    ["discovery-server"]="8761"
    ["api-gateway"]="8085"
    ["service-auth"]="8081"
    ["user-service"]="8082"
    ["service-customer"]="8083"
    ["service-product"]="8084"
    ["inventory-service"]="8086"
    ["order-service"]="8088"
    ["promotion-service"]="8087"
    ["payment-service"]="8090"
)

for service in "${!service_ports[@]}"; do
    port=${service_ports[$service]}

    if [ -d "$service" ]; then
        dockerfile_path="$service/Dockerfile"

        if [ ! -f "$dockerfile_path" ]; then
            echo "Creating Dockerfile for $service (port $port)..."

            cat > "$dockerfile_path" << EOF
FROM openjdk:21-jdk-slim

WORKDIR /app

# Copy JAR file
COPY target/*.jar app.jar

# Expose port
EXPOSE $port

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

            echo "✅ Created $dockerfile_path"
        else
            echo "⏭️  Dockerfile already exists for $service"
        fi
    else
        echo "⚠️  Directory $service not found"
    fi
done

echo ""
echo "🎉 Dockerfiles created successfully!"

