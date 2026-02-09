# Vehicle Booking Backend - Docker & Kubernetes Setup

Complete containerization and orchestration setup for automated deployment.

## 📦 What's Included

### Docker Setup
- ✅ **Multi-stage Dockerfile** - Optimized image size (~200MB)
- ✅ **Docker Compose** - MySQL + Backend + phpMyAdmin
- ✅ **Health Checks** - Automatic service monitoring
- ✅ **Security** - Non-root user, minimal base image

### Kubernetes Setup
- ✅ **Namespace** - Isolated environment
- ✅ **ConfigMap** - Environment configuration
- ✅ **Secrets** - Secure credentials management
- ✅ **MySQL** - Stateful deployment with persistent storage
- ✅ **Backend** - Scalable deployment with rolling updates
- ✅ **Service** - LoadBalancer for external access
- ✅ **HPA** - Auto-scaling based on CPU/memory
- ✅ **Ingress** - SSL/TLS support with custom domain

### Automation
- ✅ **Build Script** - Automated Docker build and push
- ✅ **Deploy Script** - One-command Kubernetes deployment
- ✅ **CI/CD Ready** - GitHub Actions template included

## 🚀 Quick Start

### Local Development (Docker Compose)

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Access:
# - Backend: http://localhost:8080
# - phpMyAdmin: http://localhost:8081
```

### Production Deployment (Kubernetes)

```bash
# Deploy to Kubernetes
./deploy-k8s.sh production

# Check status
kubectl get pods -n vehicle-booking

# Access application
kubectl port-forward svc/backend 8080:80 -n vehicle-booking
```

## 📁 File Structure

```
backend/
├── Dockerfile                  # Multi-stage build
├── .dockerignore              # Build context optimization
├── docker-compose.yml         # Local dev environment
├── .env.example               # Environment template
├── build-push.sh              # Build & push automation
├── deploy-k8s.sh              # K8s deployment script
├── DEPLOYMENT_GUIDE.md        # Comprehensive guide
├── QUICK_START.md             # Quick reference
└── k8s/                       # Kubernetes manifests
    ├── namespace.yaml         # Namespace definition
    ├── configmap.yaml         # App configuration
    ├── secrets.yaml           # Secrets (passwords, keys)
    ├── mysql-pvc.yaml         # Persistent volume
    ├── mysql-deployment.yaml  # MySQL setup
    ├── backend-deployment.yaml# Backend app
    ├── hpa.yaml               # Auto-scaling
    └── ingress.yaml           # External access
```

## 📚 Documentation

| File | Description |
|------|-------------|
| **QUICK_START.md** | Get started in 5 minutes |
| **DEPLOYMENT_GUIDE.md** | Complete deployment guide |
| **docker-compose.yml** | Local development setup |
| **k8s/*** | Kubernetes manifests |

## 🎯 Features

### Docker
- Multi-stage build for smaller images
- Health checks for MySQL and Backend
- Development tools (phpMyAdmin)
- Volume persistence
- Network isolation

### Kubernetes
- High availability (2+ replicas)
- Rolling updates with zero downtime
- Auto-scaling (2-10 pods)
- Resource limits and requests
- Liveness and readiness probes
- Secure secrets management
- SSL/TLS support

## 🔧 Configuration

### Environment Variables

Set in `.env` for Docker Compose or `k8s/configmap.yaml` for Kubernetes:

- `MYSQL_DATABASE` - Database name
- `MYSQL_USER` - Database user
- `MYSQL_PASSWORD` - Database password (secret)
- `JWT_SECRET` - JWT signing key (secret)
- `CORS_ALLOWED_ORIGINS` - Allowed CORS origins

### Scaling

```bash
# Docker Compose (limited)
docker-compose up -d --scale backend=3

# Kubernetes (recommended)
kubectl scale deployment backend --replicas=5 -n vehicle-booking

# Auto-scaling (HPA)
# Automatically scales between 2-10 pods based on CPU/memory
```

### Resource Limits

**Backend Pod:**
- Requests: 256MB RAM, 250m CPU
- Limits: 1GB RAM, 1000m CPU

**MySQL Pod:**
- Requests: 512MB RAM, 250m CPU
- Limits: 1GB RAM, 500m CPU

## 📊 Monitoring

### Health Checks

```bash
# Docker Compose
curl http://localhost:8080/actuator/health

# Kubernetes
kubectl exec <pod-name> -n vehicle-booking -- \
  curl localhost:8080/actuator/health
```

### Logs

```bash
# Docker Compose
docker-compose logs -f backend

# Kubernetes
kubectl logs -f deployment/backend -n vehicle-booking
```

### Metrics

```bash
# Kubernetes
kubectl top pods -n vehicle-booking
kubectl get hpa -n vehicle-booking
```

## 🔐 Security

- Non-root user in containers
- Minimal Alpine-based images
- Secrets management (Kubernetes secrets)
- Network policies (can be added)
- Resource limits to prevent DoS
- Health checks for automatic recovery

## 🌐 Deployment Options

### Option 1: Local Development
- Docker Desktop + Docker Compose
- Quick iteration
- Full stack on laptop

### Option 2: Minikube (Local K8s)
- Test Kubernetes locally
- Production-like environment
- Learn K8s without cloud costs

### Option 3: Cloud Kubernetes
- **Google GKE**: Managed Kubernetes
- **AWS EKS**: Elastic Kubernetes Service
- **Azure AKS**: Azure Kubernetes Service
- **DigitalOcean**: Managed Kubernetes

## 📈 Performance

### Docker Image Size
- Build stage: ~500MB (includes Maven)
- Runtime stage: ~200MB (JRE only)
- MySQL: ~500MB

### Startup Time
- MySQL: ~30 seconds
- Backend: ~60 seconds
- Total: ~90 seconds for full stack

### Resource Usage (Idle)
- MySQL: ~200MB RAM
- Backend: ~300MB RAM per pod
- Total: ~500MB for minimal setup

## 🐛 Troubleshooting

### Docker Compose
```bash
# Restart services
docker-compose restart

# Rebuild from scratch
docker-compose down -v
docker-compose up -d --build

# Check logs
docker-compose logs backend
```

### Kubernetes
```bash
# Check pod status
kubectl get pods -n vehicle-booking

# Describe pod
kubectl describe pod <pod-name> -n vehicle-booking

# View logs
kubectl logs <pod-name> -n vehicle-booking

# Delete and recreate
kubectl delete pod <pod-name> -n vehicle-booking
```

## 🔄 Updates

### Rolling Updates

```bash
# Build new version
./build-push.sh v1.1.0

# Deploy to Kubernetes
kubectl set image deployment/backend \
  backend=your-registry/vehicle-booking-backend:v1.1.0 \
  -n vehicle-booking

# Watch rollout
kubectl rollout status deployment/backend -n vehicle-booking

# Rollback if needed
kubectl rollout undo deployment/backend -n vehicle-booking
```

## 📞 Support

### Common Issues

1. **Port conflicts**: Check with `lsof -i :PORT` and stop conflicting services
2. **Disk space**: Clean with `docker system prune -a`
3. **Permission denied**: Check file permissions and Docker daemon
4. **Image pull errors**: Verify registry credentials

### Get Help

- Check logs first: `docker-compose logs` or `kubectl logs`
- Review documentation: `DEPLOYMENT_GUIDE.md`
- Check resource limits: `docker stats` or `kubectl top pods`

## ✅ Checklist

**Before Deploying to Production:**

- [ ] Change default passwords in `k8s/secrets.yaml`
- [ ] Update domain in `k8s/ingress.yaml`
- [ ] Set up SSL/TLS certificates
- [ ] Configure backup strategy
- [ ] Set proper resource limits
- [ ] Configure monitoring
- [ ] Test disaster recovery
- [ ] Review security policies
- [ ] Set up log aggregation
- [ ] Test rolling updates

## 🎓 Learn More

- **Docker**: https://docs.docker.com
- **Kubernetes**: https://kubernetes.io/docs
- **Spring Boot on K8s**: https://spring.io/guides/gs/spring-boot-kubernetes

---

## 📝 License

MIT License - see LICENSE file for details

## 👥 Contributors

Vehicle Booking Team

---

**Ready to deploy!** 🚀

Start with local development using Docker Compose, then scale to production with Kubernetes.

For detailed instructions, see:
- **QUICK_START.md** - Get started in 5 minutes
- **DEPLOYMENT_GUIDE.md** - Complete reference guide
