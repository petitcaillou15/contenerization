# Mini réseau social sur Kubernetes — Groupe Horion, La, Fayala

## Membres du groupe

| Prénom | Nom | Email | ID GitLab |
|--------|-----|-------|-----------|
| Antoine | Horion | antoine.horion17@gmail.com | horion.antoine |
| Maxime | LA | la.maxime2004@gmail.com | la.maxime |
| Hakim | Fayala | hfayala53@gmail.com | hakim.fayala |

## Présentation

Le mini réseau social (inscription, posts, commentaires, notifications) est déployé sur Kubernetes avec minikube. Ses 6 composants (`users`, `posts`, `notifications`, `gateway`, `frontend`, `redis`) tournent dans le Namespace `mini-reseau`. La base PostgreSQL tourne hors du cluster, dans un `docker-compose` séparé.


## Schéma de l'architecture

## Prérequis

- Docker et Docker Compose
- minikube et `kubectl`
- git
- un navigateur


## Environnement Kubernetes utilisé

minikube avec le driver Docker. Nous avons d'abord travaillé sur la VM Ubuntu 24.04 de l'école. En fin de projet, nous sommes passés sous WSL (Ubuntu, avec Docker Desktop). Les deux environnements fonctionnent. La seule différence concerne l'accès à l'application (section 4).

Les images sont construites directement dans minikube. La base PostgreSQL tourne dans Docker sur la même machine, et les Pods la joignent par `host.minikube.internal`.

Cloner le dépôt, puis se placer dans le dossier `projet/`. **Toutes les commandes ci-dessous se lancent depuis ce dossier.**

```bash
git clone https://devops.telecomste.fr/fayala.hakim/fi3_td_kube.git
cd fi3_td_kube/projet
```

## 1. Démarrer la base de données

Créer d'abord les deux fichiers de configuration, à partir de leurs modèles :

```bash
cp db/.env.example db/.env              # l'administrateur de la base
cp k8s/.env.example k8s/.env.secrets    # les mots de passe des services et JWT_SECRET
```

Remplacer chaque `a_remplir` dans les deux fichiers. Le fichier `k8s/.env.secrets` est lu à la fois par la base et par Kubernetes : les mots de passe ne sont écrits qu'une fois.

Puis lancer la base :

```bash
cd db
docker compose up -d
docker ps --filter name=base-postgres    # doit afficher (healthy)
cd ..
```

Au premier démarrage, `db/00-init.sql` crée un utilisateur et un schéma par service, avec les mots de passe de `k8s/.env.secrets`.

## 2. Construire les images

Démarrer minikube, puis construire les 5 images d'une version avec le script `build_images.sh`. Chaque version se construit depuis sa branche :

```bash
minikube start

git switch --detach v1.0.0  
./scripts/build_images.sh 1.0.0         # Version initiale

git switch --detach v2.0.0
./scripts/build_images.sh 2.0.0         # Version évoluée

git switch --detach v3.0.0Correction
./scripts/build_images.sh 3.0.0C        # Version avec la limite de caractères

git switch --detach v3.0.0Rollback
./scripts/build_images.sh 3.0.0R        # Version KO

git switch main                         # revenir sur main avant de déployer
minikube image ls | grep mini-reseau    # les 20 images doivent apparaître
```

## 3. Déployer l'application

**3.1 Activer l'Ingress de minikube**

```bash
minikube addons enable ingress
```

**3.2 Déployer la version initiale**

```bash
./scripts/deploy.sh
```

Le script crée le Namespace et le Secret, applique la configuration, les Services, les Deployments et l'Ingress. Il démarre les services Java un par un, en attendant que chacun soit prêt. Il faut compter quelques minutes.

## 4. Accéder à l'application

Associer le nom `mini-reseau.local` à l'adresse de minikube :

```bash
echo "$(minikube ip)  mini-reseau.local" | sudo tee -a /etc/hosts
```

Puis ouvrir **http://mini-reseau.local** dans un navigateur.

Comptes de démonstration : `alice@example.com` ou `bob@example.com`, mot de passe `password`.

**Sous WSL avec Docker Desktop**, l'adresse de minikube n'est pas joignable. Utiliser plutôt `127.0.0.1` dans `/etc/hosts`, et laisser tourner `minikube tunnel` dans un second terminal :

```bash
echo "127.0.0.1  mini-reseau.local" | sudo tee -a /etc/hosts
minikube tunnel
```

## 5. Vérifier rapidement que ça fonctionne

```bash
kubectl get pods -n mini-reseau                       # 8 Pods en 1/1 Running
kubectl logs -n mini-reseau deploy/posts --tail=10    # les logs d'un composant
```

Dans le navigateur : se connecter, publier un post, le voir apparaître dans le fil.

Une métrique, le nombre de posts créés :

```bash
kubectl run test-curl -n mini-reseau --rm -it --image=curlimages/curl -- sh
curl -s http://posts:8080/metrics | grep posts_total
exit
```

## 6. Passer d'une version à l'autre

Les images de la version doivent avoir été construites (section 2). Le script `switch_version.sh` met à jour les 5 services un par un :

```bash
./scripts/switch_version.sh 2.0.0     # version évoluée
kubectl get deployments -n mini-reseau \
-o custom-columns=NAME:.metadata.name,IMAGE:.spec.template.spec.containers[*].image       # Vérifier les versions qui tournent   

./scripts/switch_version.sh 1.0.0     # retour à la version initiale
kubectl get deployments -n mini-reseau \
-o custom-columns=NAME:.metadata.name,IMAGE:.spec.template.spec.containers[*].image       # Vérifier les versions qui tournent   
```

**Version Correction (3.0.0)** : Il faut se placer dans le tag v3.0.0Correction

```bash
git switch --detach v3.0.0Correction
./scripts/switch_version.sh 3.0.0C     # Version avec longueure maximale du post
kubectl get deployments -n mini-reseau \
-o custom-columns=NAME:.metadata.name,IMAGE:.spec.template.spec.containers[*].image       # Vérifier les versions qui tournent   
```

**Version KO (3.0.0)** : le service `posts` ne démarre pas.

```bash
./scripts/switch_version.sh 3.0.0R     # posts reste en erreur : arrêter le script avec Ctrl+C
```

Diagnostic :

```bash
kubectl get pods -n mini-reseau                    # posts en CrashLoopBackOff
kubectl describe pod <pod-posts> -n mini-reseau    # redémarrages en boucle
kubectl logs <pod-posts> -n mini-reseau            # la variable POSTS_MAX_CONTENT_LENGTH manque
```

Revenir à la version précédente (rollback) :

```bash
kubectl rollout undo deployment/posts -n mini-reseau
```

Pendant toute la panne, l'ancien Pod `posts` reste en service : l'application continue de fonctionner.

## 7. Tout supprimer

```bash
kubectl delete namespace mini-reseau     # supprime toute l'application
cd db
docker compose down                      # arrête la base, en gardant ses données
```

## Structure du dépôt

| Dossier | Contenu |
|---|---|
| `projet/services/`, `projet/gateway/`, `projet/frontend/` | le code et le Dockerfile de chaque composant |
| `projet/k8s/` | les manifestes Kubernetes |
| `projet/db/` | le `docker-compose.yml` de la base et son script d'initialisation |
| `projet/scripts/` | les scripts de construction, de déploiement et de changement de version |
| `compte-rendu/` | le compte rendu, une partie par fichier |