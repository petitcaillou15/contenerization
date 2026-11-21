#!/usr/bin/env bash
# Déploie (ou met à jour) l'application dans le Namespace mini-reseau.
# Prérequis : minikube démarré, base de données lancée, images construites,
#             fichier k8s/.env.secrets présent.

set -e                       # s'arrête à la première erreur

# Namespace
kubectl apply -f k8s/namespace.yaml

# Secret, seulement s'il n'existe pas encore
if ! kubectl get secret app-secrets -n mini-reseau > /dev/null 2>&1; then
  kubectl create secret generic app-secrets -n mini-reseau --from-env-file=k8s/.env.secrets
fi

# Configuration
kubectl apply -f k8s/app-config.yaml
kubectl apply -f k8s/postgres-external.yaml
kubectl apply -f k8s/users-service.yaml
kubectl apply -f k8s/posts-service.yaml
kubectl apply -f k8s/notifications-service.yaml
kubectl apply -f k8s/gateway-service.yaml
kubectl apply -f k8s/frontend-service.yaml
kubectl apply -f k8s/redis-service.yaml
kubectl apply -f k8s/redis-deployment.yaml

# Users, posts, notifications
kubectl apply -f k8s/users-deployment.yaml
kubectl rollout status deployment/users -n mini-reseau

kubectl apply -f k8s/posts-deployment.yaml
kubectl rollout status deployment/posts -n mini-reseau

kubectl apply -f k8s/notifications-deployment.yaml
kubectl rollout status deployment/notifications -n mini-reseau

# Gateway et le frontend
kubectl apply -f k8s/gateway-deployment.yaml
kubectl apply -f k8s/frontend-deployment.yaml
kubectl rollout status deployment/gateway -n mini-reseau
kubectl rollout status deployment/frontend -n mini-reseau

# Ingress
kubectl apply -f k8s/ingress.yaml

# État final
kubectl get pods -n mini-reseau