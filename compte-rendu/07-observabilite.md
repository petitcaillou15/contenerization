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

## Pourquoi ces choix

- **`kubectl logs`** : c'est la façon standard de lire les logs d'un Pod.
- **L'endpoint `/metrics`** : l'application l'expose déjà. Une métrique métier comme `posts_total` montre ce que fait l'application, ce qui est plus parlant que la consommation de CPU ou de mémoire.
- **Un Pod de test** : le Service `posts` n'est pas exposé hors du cluster, et on ne voulait pas l'exposer juste pour lire ses métriques.

## Difficultés rencontrées

- **Le nom de la métrique.** Dans le code, le compteur s'appelle `posts_created_total`, mais il est publié sous le nom `posts_total` : la bibliothèque de métriques a modifié le nom. On l'a retrouvé en cherchant la description du compteur (« Nombre total de posts créés ») dans la sortie de `/metrics`.
- **Le compteur ne correspond pas au nombre de posts du site.** Le site affichait 4 posts, mais le compteur valait 1. Le compteur est gardé en mémoire par le Pod : il ne compte que les posts créés **par ce Pod depuis son démarrage**, et repart de 0 à chaque redémarrage. Les autres posts existaient déjà en base.

## Éléments à relire

- Les logs disparaissent avec le Pod qui les a produits : il faudrait les centraliser pour les garder.
