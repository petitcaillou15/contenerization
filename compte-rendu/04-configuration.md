# 4. Configuration

**Statut** : traité

## Ce qui a été fait

**ConfigMap : quelles valeurs, injectées comment**

Un seul ConfigMap (`k8s/app-config.yaml`) avec la configuration non sensible partagée par les services :

| Clé | Valeur | Rôle |
|---|---|---|
| `POSTGRES_DB` | `social` | nom de la base |
| `USERS_SERVICE_URL` | `http://users:8080` | adresse de `users` pour vérifier les jetons |
| `USERS_HTTP_TIMEOUT_MS` | `1000` | délai maximum de cet appel |
| `REDIS_HOST` / `REDIS_PORT` | `redis` / `6379` | adresse de Redis |
| `JWT_EXPIRATION` | `86400` | durée de validité d'un jeton (24 h) |
| `POSTS_MAX_CONTENT_LENGTH` | `280` | nombre maximum de caractères dans un post (uniquement dans la version 3.0.0 corrigée) | 

Ces valeurs arrivent dans les conteneurs sous forme de **variables d'environnement**. Dans chaque Deployment Java, `envFrom` charge tout le ConfigMap d'un coup.

**Secret : lesquels, et comment ils sont gérés dans le dépôt**

Un Secret, `app-secrets`, avec 4 valeurs : les mots de passe de la base des 3 services (`USERS_DB_PASSWORD`, `POSTS_DB_PASSWORD`, `NOTIFICATIONS_DB_PASSWORD`) et `JWT_SECRET`, la clé qui sert à signer les jetons de connexion.

Le Secret **n'est pas dans le dépôt**. Ses valeurs sont dans un fichier local `k8s/.env.secrets`, ignoré par Git, et on crée le Secret à partir de ce fichier. Seul un modèle sans valeurs, `k8s/.env.example`, est versionné. Ce même fichier fournit aussi les mots de passe à la base de données (voir partie 6) : ils ne sont écrits qu'à un seul endroit.

**Preuve qu'on change la configuration sans reconstruire d'image**

On a changé `JWT_EXPIRATION` de `86400` à `3600` dans le ConfigMap, puis redémarré `users`. La valeur a changé, et l'image est restée exactement la même (même identifiant).

## Comment

**Créer le ConfigMap**

```bash
kubectl apply -f k8s/app-config.yaml
```
**Créer le fichier des secrets** à partir du modèle, puis remplacer chaque `a_remplir` :

```bash
cp k8s/.env.example k8s/.env.secrets
```

**Créer le Secret** à partir du fichier local :

```bash
kubectl create secret generic app-secrets -n mini-reseau --from-env-file=k8s/.env.secrets
```

**Les utiliser dans un Deployment** (extrait de `k8s/users-deployment.yaml`) :

```yaml
envFrom:
  - configMapRef:
      name: app-config          # tout le ConfigMap
env:
  - name: JWT_SECRET
    valueFrom:
      secretKeyRef:
        name: app-secrets       # une seule clé du Secret
        key: JWT_SECRET
```

**La démonstration**, étape par étape :

```bash
# état de départ
kubectl exec -n mini-reseau deploy/users -- printenv JWT_EXPIRATION
86400
kubectl describe pod -n mini-reseau -l app=users | grep "Image ID"
    Image ID:       sha256:6a1fe772edcad02fddeea8ce00b6c169b2425169cce83b30b2d35163fc3820bb

# on change 86400 en 3600 dans k8s/app-config.yaml, puis :
kubectl apply -f k8s/app-config.yaml
kubectl rollout restart deployment/users -n mini-reseau

# après le redémarrage
kubectl exec -n mini-reseau deploy/users -- printenv JWT_EXPIRATION
3600
kubectl describe pod -n mini-reseau -l app=users | grep "Image ID"
    Image ID:       sha256:6a1fe772edcad02fddeea8ce00b6c169b2425169cce83b30b2d35163fc3820bb
```

On a ensuite remis la valeur d'origine de la même façon.

### Configuration spécifique à la version 3.0.0

La version 3.0.0 du service `posts` introduit une limite
de longueur pour le contenu des publications.

Cette limite est définie par la variable d'environnement
`POSTS_MAX_CONTENT_LENGTH`.

Dans notre configuration, cette variable est définie
à 280 caractères dans le ConfigMap `app-config` :

```yaml
POSTS_MAX_CONTENT_LENGTH: "280"
```

La variable est obligatoire : l'application ne définit
pas de valeur par défaut.

Dans le fichier `application.yml` du service `posts`,
la propriété est associée à cette variable :

```yaml
posts:
  max-content-length: ${POSTS_MAX_CONTENT_LENGTH}
```

Lors du passage en version 3.0.0, le script
`switch_version.sh` réapplique le ConfigMap avant
de mettre à jour le Deployment `posts`.

Cela permet au nouveau conteneur de récupérer
la configuration nécessaire à son démarrage.

La limite peut être vérifiée dans le conteneur
avec la commande :

```bash
kubectl exec -n mini-reseau deploy/posts -- \
  printenv POSTS_MAX_CONTENT_LENGTH
```

La valeur attendue est :

```text
280
```

La configuration du ConfigMap peut également être
consultée directement depuis Kubernetes :

```bash
kubectl get configmap app-config -n mini-reseau -o yaml
```

Cette commande permet de vérifier que la variable
`POSTS_MAX_CONTENT_LENGTH` est bien présente
et possède la valeur attendue.

## Pourquoi ces choix

- **Un seul fichier pour la base et le Secret** : la base crée les comptes des services avec ces mots de passe, et les services s'y connectent avec les mêmes. Les lire au même endroit évite qu'ils divergent.
- **Un seul ConfigMap partagé (`app-config`)** : les 3 services Java utilisent les mêmes valeurs (nom de la base, adresse de Redis, adresse de `users`). Les écrire une seule fois évite qu'elles divergent.
- **`rollout restart` après un changement** : les variables d'environnement sont lues au démarrage du conteneur. Modifier le ConfigMap ne suffit pas, il faut redémarrer les Pods.

## Difficultés rencontrées

Pas de difficultés remarquables.

## Éléments à relire
