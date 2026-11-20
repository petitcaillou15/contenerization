# Nom du projet — Groupe X

> Modèle de README. Copiez-le à la racine du dépôt (à la place du `README.md` actuel) et complétez-le.
> Une personne extérieure au groupe doit pouvoir **construire, déployer, accéder et vérifier** en suivant ce document.

## Membres du groupe

| Prénom | Nom | Email | ID GitLab |
|--------|-----|-------|-----------|
|        |     |       |           |

## Présentation

Courte description de l'application et de ce que vous avez mis en place.

## Schéma de l'architecture

![Architecture finale](chemin/vers/le-schema.png)

## Prérequis

Outils et versions nécessaires (Docker, kubectl...), état attendu du poste.

## Environnement Kubernetes utilisé

Précisez **votre environnement** (minikube sur la VM de l'école ou sur Linux, Docker Desktop, kind, k3s, cluster cloud, service managé...), sa version, et ce qu'il faut pour le reproduire (accès, registry, où tourne la base de données).

## 1. Démarrer la base de données

Commandes pour lancer le `docker-compose` séparé de la base, et comment vérifier qu'elle fonctionne.

## 2. Construire les images

Commandes exactes, pour **chaque** image et pour **chaque** version (initiale, évoluée, KO), avec les tags.

## 3. Déployer l'application

Ordre et commandes de déploiement (Namespace, configuration, secrets, Deployments, Services, Ingress...).

## 4. Accéder à l'application

URL, éventuelle configuration de `/etc/hosts`, identifiants de démonstration.

## 5. Vérifier rapidement que ça fonctionne

Les commandes ou actions qui prouvent le bon fonctionnement (Pods prêts, application accessible, logs, une métrique...).

## 6. Passer d'une version à l'autre

Comment déployer la version évoluée, la version KO, et revenir en arrière.

## 7. Tout supprimer

Comment supprimer complètement le Namespace (et la base) pour repartir de zéro.

## Structure du dépôt

Où se trouvent les Dockerfiles, les manifests, les scripts et le compte rendu.
