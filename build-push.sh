#!/bin/bash

# Docker Build and Push Script
# Usage: ./build-push.sh [version]

set -e

# Configuration
DOCKER_REGISTRY="your-registry.com"  # Update with your registry
IMAGE_NAME="vehicle-booking-backend"
VERSION=${1:-latest}

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}Building Docker image...${NC}"

# Build the Docker image
docker build -t ${IMAGE_NAME}:${VERSION} .

# Tag for registry
docker tag ${IMAGE_NAME}:${VERSION} ${DOCKER_REGISTRY}/${IMAGE_NAME}:${VERSION}
docker tag ${IMAGE_NAME}:${VERSION} ${DOCKER_REGISTRY}/${IMAGE_NAME}:latest

echo -e "${GREEN}Docker image built successfully!${NC}"
echo -e "${YELLOW}Image: ${DOCKER_REGISTRY}/${IMAGE_NAME}:${VERSION}${NC}"

# Login to Docker registry (uncomment and configure)
# echo -e "${GREEN}Logging in to Docker registry...${NC}"
# docker login ${DOCKER_REGISTRY}

# Push to registry
echo -e "${GREEN}Pushing image to registry...${NC}"
docker push ${DOCKER_REGISTRY}/${IMAGE_NAME}:${VERSION}
docker push ${DOCKER_REGISTRY}/${IMAGE_NAME}:latest

echo -e "${GREEN}✓ Image pushed successfully!${NC}"
echo -e "${YELLOW}To deploy: kubectl set image deployment/backend backend=${DOCKER_REGISTRY}/${IMAGE_NAME}:${VERSION} -n vehicle-booking${NC}"
