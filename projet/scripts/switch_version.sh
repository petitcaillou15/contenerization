#!/bin/bash

set -euo pipefail

VERSION="${1:-}"

if [ -z "$VERSION" ]; then
    echo "Usage : $0 <version> (ex : $0 1.0.0)"
    exit 1
fi

NAMESPACE="mini-reseau"

# Chemins calculés à partir de l'emplacement du script
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
CONFIG_FILE="$PROJECT_DIR/k8s/app-config.yaml"

switch() {
    SERVICE="$1"

    echo "Mise à jour de $SERVICE vers la version $VERSION..."

    kubectl set image "deployment/$SERVICE" \
        "$SERVICE=mini-reseau-social-$SERVICE:$VERSION" \
        -n "$NAMESPACE"

    kubectl rollout status "deployment/$SERVICE" \
        -n "$NAMESPACE"
}

if [ "$VERSION" = "3.0.0" ]; then

    echo "Passage en V3 : réapplication de app-config.yaml..."

    kubectl apply -f "$CONFIG_FILE" -n "$NAMESPACE"

    echo "Mise à jour du service posts uniquement..."
    switch posts

else

    echo "Passage en version $VERSION : mise à jour de tous les services..."

    switch users
    switch posts
    switch notifications
    switch gateway
    switch frontend

fi

echo "Changement de version terminé."
