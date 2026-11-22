
#!/bin/bash

set -euo pipefail

VERSION="${1:-}"

# S'arrête s'il y a une erreur
if [ -z "$VERSION" ]; then
    echo "Usage : $0 <version> (ex : $0 1.0.0)"
    exit 1
fi

NAMESPACE="mini-reseau"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
CONFIG_FILE="$PROJECT_DIR/k8s/app-config.yaml"

# Vérifie que le Deployment utilise bien la version attendue.
check_version() {
    SERVICE="$1"
    EXPECTED_IMAGE="mini-reseau-social-$SERVICE:2.0.0"

    echo "Vérification de $SERVICE..."

    if ! kubectl get deployment "$SERVICE" -n "$NAMESPACE" >/dev/null 2>&1; then
        echo "ERREUR : le Deployment $SERVICE est introuvable."
        exit 1
    fi

    CURRENT_IMAGE=$(kubectl get deployment "$SERVICE" \
        -n "$NAMESPACE" \
        -o jsonpath='{.spec.template.spec.containers[0].image}')

    if [ "$CURRENT_IMAGE" != "$EXPECTED_IMAGE" ]; then
        echo "ERREUR : $SERVICE n'est pas en version 2.0.0."
        echo "Image actuelle : $CURRENT_IMAGE"
        echo "Image attendue : $EXPECTED_IMAGE"
        echo "Passage en V3 annulé."
        exit 1
    fi

    echo "$SERVICE est bien en version 2.0.0."
}

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

    echo "Passage en V3 : vérification des versions préalables..."

    # En V3, seuls les posts doivent changer.
    check_version users
    check_version notifications
    check_version gateway
    check_version frontend

    echo "Les quatre autres services sont bien en V2."

    echo "Réapplication de app-config.yaml..."
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
