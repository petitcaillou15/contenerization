
#!/bin/bash

set -euo pipefail

VERSION="${1:-}"

if [ -z "$VERSION" ]; then
    echo "Usage : $0 <version> (ex : $0 1.0.0)"
    exit 1
fi

NAMESPACE="mini-reseau"

switch() {
    SERVICE="$1"

    echo "Mise à jour de $SERVICE vers la version $VERSION..."

    kubectl set image "deployment/$SERVICE" \
        "$SERVICE=mini-reseau-social-$SERVICE:$VERSION" \
        -n "$NAMESPACE"

    kubectl rollout status "deployment/$SERVICE" \
        -n "$NAMESPACE"
}

switch users
switch posts
switch notifications
switch gateway
switch frontend

echo "Tous les services utilisent la version $VERSION."
