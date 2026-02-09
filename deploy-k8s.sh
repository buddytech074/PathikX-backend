#!/bin/bash

# Kubernetes Deployment Script
# Usage: ./deploy-k8s.sh [environment]

set -e

ENVIRONMENT=${1:-production}

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Vehicle Booking Backend Deployment${NC}"
echo -e "${BLUE}  Environment: ${ENVIRONMENT}${NC}"
echo -e "${BLUE}========================================${NC}"

# Create namespace
echo -e "\n${GREEN}Creating namespace...${NC}"
kubectl apply -f k8s/namespace.yaml

# Wait for namespace to be ready
kubectl wait --for=jsonpath='{.status.phase}'=Active namespace/vehicle-booking --timeout=30s

# Create ConfigMap and Secrets
echo -e "\n${GREEN}Creating ConfigMap...${NC}"
kubectl apply -f k8s/configmap.yaml

echo -e "\n${YELLOW}Creating Secrets...${NC}"
echo -e "${YELLOW}WARNING: Update k8s/secrets.yaml with production values before deploying!${NC}"
kubectl apply -f k8s/secrets.yaml

# Create Firebase credentials secret (if firebase-credentials.json exists)
if [ -f "firebase-credentials.json" ]; then
    echo -e "\n${GREEN}Creating Firebase credentials secret...${NC}"
    kubectl create secret generic firebase-credentials \
        --from-file=firebase-credentials.json \
        --namespace=vehicle-booking \
        --dry-run=client -o yaml | kubectl apply -f -
else
    echo -e "\n${YELLOW}Warning: firebase-credentials.json not found!${NC}"
    echo -e "${YELLOW}You need to manually create the Firebase secret:${NC}"
    echo -e "${YELLOW}kubectl create secret generic firebase-credentials --from-file=firebase-credentials.json -n vehicle-booking${NC}"
fi

# Deploy MySQL
echo -e "\n${GREEN}Deploying MySQL...${NC}"
kubectl apply -f k8s/mysql-pvc.yaml
kubectl apply -f k8s/mysql-deployment.yaml

# Wait for MySQL to be ready
echo -e "\n${GREEN}Waiting for MySQL to be ready...${NC}"
kubectl wait --for=condition=available --timeout=300s deployment/mysql -n vehicle-booking

# Deploy Backend
echo -e "\n${GREEN}Deploying Backend application...${NC}"
kubectl apply -f k8s/backend-deployment.yaml

# Wait for backend to be ready
echo -e "\n${GREEN}Waiting for Backend to be ready...${NC}"
kubectl wait --for=condition=available --timeout=300s deployment/backend -n vehicle-booking

# Deploy HPA
echo -e "\n${GREEN}Deploying HorizontalPodAutoscaler...${NC}"
kubectl apply -f k8s/hpa.yaml

# Deploy Ingress (optional)
if [ -f "k8s/ingress.yaml" ]; then
    echo -e "\n${GREEN}Deploying Ingress...${NC}"
    kubectl apply -f k8s/ingress.yaml
fi

# Show deployment status
echo -e "\n${BLUE}========================================${NC}"
echo -e "${BLUE}  Deployment Status${NC}"
echo -e "${BLUE}========================================${NC}"

echo -e "\n${GREEN}Pods:${NC}"
kubectl get pods -n vehicle-booking

echo -e "\n${GREEN}Services:${NC}"
kubectl get svc -n vehicle-booking

echo -e "\n${GREEN}Ingress:${NC}"
kubectl get ingress -n vehicle-booking 2>/dev/null || echo "No ingress configured"

echo -e "\n${BLUE}========================================${NC}"
echo -e "${GREEN}✓ Deployment complete!${NC}"
echo -e "${BLUE}========================================${NC}"

# Show useful commands
echo -e "\n${YELLOW}Useful commands:${NC}"
echo -e "  View logs:           kubectl logs -f deployment/backend -n vehicle-booking"
echo -e "  View pod status:     kubectl get pods -n vehicle-booking"
echo -e "  Describe pod:        kubectl describe pod <pod-name> -n vehicle-booking"
echo -e "  Port forward:        kubectl port-forward svc/backend 8080:80 -n vehicle-booking"
echo -e "  Scale deployment:    kubectl scale deployment backend --replicas=3 -n vehicle-booking"
echo -e "  Delete deployment:   kubectl delete namespace vehicle-booking"
