#!/usr/bin/env bash


#Construit les 5 images de l'application dans minikube.
#Usage : ./scripts/build-images.sh 1.0.0

set -e                       # s'arrête à la première erreur

VERSION="$1"
if [ -z "$VERSION" ]; then
  echo "Usage : $0 <version>   (ex : $0 1.0.0)"
  exit 1
fi


#nom du composant -> dossier contenant son Dockerfile

build() {
  echo "=== $1:$VERSION ($2)"
  minikube image build -t "mini-reseau-social-$1:$VERSION" "$2"
}

build users         services/users
build posts         services/posts
build notifications services/notifications
build gateway       gateway
build frontend      frontend

echo "Images $VERSION construites :"
minikube image ls | grep "mini-reseau-social.*:$VERSION"