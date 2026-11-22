# 7. Observabilité

**Statut** : traité

## Ce qui a été fait

**Afficher les logs d'un composant**

Avec `kubectl logs`. Exemple pour `posts` :

```bash
kubectl logs -n mini-reseau deploy/posts --tail=10
```

On y voit par exemple le démarrage du service :

```
... Tomcat started on port 8080 (http) with context path '/'
... Started PostsApplication in 34.636 seconds (process running for 39.32)
```

**Une métrique : `posts_total`**

Le service `posts` publie ses métriques sur `/metrics`. On utilise `posts_total`, le nombre de posts créés : c'est une **métrique métier**, définie dans le code de `posts`.

On la lit avec `curl http://<service>/metrics`, depuis un Pod de test placé dans le cluster. On a vérifié qu'elle augmente quand on publie un post.

## Comment

**Ouvrir un terminal dans un Pod de test qui contient `curl`** (le nom `posts` n'est connu qu'à l'intérieur du cluster) :

```bash
kubectl run test-curl -n mini-reseau --rm -it --image=curlimages/curl -- sh
```

**Lire la métrique, publier un post dans le navigateur, puis la relire :**

```
~ $ curl -s http://posts:8080/metrics | grep posts_total
# HELP posts_total Nombre total de posts créés
# TYPE posts_total counter
posts_total 1.0

(publication d'un post sur http://mini-reseau.local)

~ $ curl -s http://posts:8080/metrics | grep posts_total
# HELP posts_total Nombre total de posts créés
# TYPE posts_total counter
posts_total 2.0
```


## Vérification des versions applicatives

Les services `users`, `posts` et `notifications`
exposent un endpoint `GET /version`.

Ces endpoints permettent de connaître la version
applicative actuellement exécutée par chaque service.

La gateway permet d'accéder à ces informations
sans contacter directement les Pods.

Les routes suivantes sont disponibles :

```text
GET /api/users/version
GET /api/posts/version
GET /api/notifications/version
```

Ces routes permettent notamment de vérifier
le passage entre les versions 1.0.0 et 2.0.0.

Lors du déploiement de la version 3.0.0 (correction),
le service `posts` doit retourner la version 3.0.0,
tandis que les services `users` et `notifications`
doivent toujours retourner la version 2.0.0.

## V3 Correction
### Tests fonctionnels de la version 3.0.0

La version 3.0.0 du service `posts` introduit
une limite maximale de 280 caractères pour le contenu
d'une publication.

La limite est configurée grâce à la variable
d'environnement `POSTS_MAX_CONTENT_LENGTH`.

Trois cas possibles :

| Longueur du contenu | Résultat attendu |
|---|---|
| 100 caractères | Publication acceptée |
| 280 caractères | Publication acceptée |
| 281 caractères | Publication refusée avec HTTP 400 |

Lorsqu'un contenu dépasse la limite, le service
retourne une réponse HTTP 400.

Ces tests permettent de vérifier que la règle métier
est bien appliquée par le backend et pas uniquement
par l'interface frontend.

Le compteur de caractères du frontend constitue
une aide à la saisie, mais le contrôle du backend
reste indispensable pour empêcher l'envoi d'un contenu
trop long par un autre client HTTP.


### Consultation des logs pendant un changement de version

Les logs des Pods permettent de suivre le démarrage
des services et de diagnostiquer les éventuelles erreurs.

Pour consulter les logs du service `posts` :

```bash
kubectl logs -n mini-reseau deployment/posts
```

Pour suivre les logs en temps réel :

```bash
kubectl logs -n mini-reseau deployment/posts -f
```

Après un changement de version, ces commandes permettent
de vérifier le démarrage de la nouvelle instance
et de rechercher d'éventuelles erreurs de configuration.

### Consultation des métriques

Le service `posts` expose également un endpoint
de métriques compatible avec le format Prometheus :

```text
/metrics
```

Cet endpoint permet de consulter les métriques
exposées par Spring Boot Actuator et Micrometer.

Pour vérifier que l'endpoint répond, il est possible
d'utiliser un port-forward vers le service `posts` :

```bash
kubectl port-forward -n mini-reseau service/posts 8082:8080
```

Puis, dans un autre terminal :

```bash
curl http://localhost:8082/metrics
```

La réponse permet de consulter les métriques
exposées par le service.

## V3 rollback
### Diagnostic d'un déploiement défaillant (V3)

Nous avons déployé la version 3 du service `posts`. Cette version présente volontairement un problème de configuration : la variable d'environnement `POSTS_MAX_CONTENT_LENGTH` est absente, alors que l'application Spring Boot en a besoin pour démarrer.

Nous avons suivi la démarche de diagnostic demandée : `get` → `describe` → `logs`.

### 1. Observation des Pods avec `kubectl get`

```bash
kubectl get pods -n mini-reseau
```

Le résultat obtenu montre que le Pod du service `posts` ne démarre pas correctement :

```text
posts-c8f6544ff-7rdbs   0/1   CrashLoopBackOff   4 (24s ago)   4m12s
```

L'état `CrashLoopBackOff` indique que le conteneur échoue à démarrer et que Kubernetes tente régulièrement de le redémarrer.

### 2. Inspection du Deployment avec `kubectl describe`

```bash
kubectl describe deployment posts -n mini-reseau
```

Les informations importantes relevées sont :

```text
Replicas: 1 desired | 1 updated | 2 total | 1 available | 1 unavailable
```

Ces informations montrent que le déploiement est dans un état transitoire : un Pod est disponible, mais un autre ne l'est pas. Le nouveau déploiement n'est donc pas complètement opérationnel.

### 3. Analyse des logs avec `kubectl logs`

Nous avons ensuite consulté les logs du conteneur pour identifier la cause exacte de l'erreur.

Le message important obtenu est :

```text
Caused by: org.springframework.util.PlaceholderResolutionException:
Could not resolve placeholder 'POSTS_MAX_CONTENT_LENGTH'
in value "${POSTS_MAX_CONTENT_LENGTH}"
<-- "${posts.max-content-length}"
```

Cette erreur indique que Spring Boot ne parvient pas à résoudre la variable d'environnement `POSTS_MAX_CONTENT_LENGTH`, utilisée pour configurer `posts.max-content-length`.

### 4. Cause identifiée

Le problème ne vient pas directement de Kubernetes, mais de la configuration de l'application : une variable d'environnement obligatoire est absente.

L'application Spring Boot ne peut donc pas terminer son initialisation. Le conteneur échoue et entre dans l'état `CrashLoopBackOff`.

Ce diagnostic nous a permis d'identifier précisément la cause du dysfonctionnement de la V3 avant de procéder au rollback.
de procéder au rollback.


## Pourquoi ces choix

- **`kubectl logs`** : c'est la façon standard de lire les logs d'un Pod.
- **L'endpoint `/metrics`** : l'application l'expose déjà. Une métrique métier comme `posts_total` montre ce que fait l'application, ce qui est plus parlant que la consommation de CPU ou de mémoire.
- **Un Pod de test** : le Service `posts` n'est pas exposé hors du cluster, et on ne voulait pas l'exposer juste pour lire ses métriques.

## Difficultés rencontrées

- **Le nom de la métrique.** Dans le code, le compteur s'appelle `posts_created_total`, mais il est publié sous le nom `posts_total` : la bibliothèque de métriques a modifié le nom. On l'a retrouvé en cherchant la description du compteur (« Nombre total de posts créés ») dans la sortie de `/metrics`.
- **Le compteur ne correspond pas au nombre de posts du site.** Le site affichait 4 posts, mais le compteur valait 1. Le compteur est gardé en mémoire par le Pod : il ne compte que les posts créés **par ce Pod depuis son démarrage**, et repart de 0 à chaque redémarrage. Les autres posts existaient déjà en base.

## Éléments à relire

- Les logs disparaissent avec le Pod qui les a produits : il faudrait les centraliser pour les garder.
