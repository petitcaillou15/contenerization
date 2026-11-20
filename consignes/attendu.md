# Projet Kubernetes — Application microservices

## Objectif

Vous disposez d'une application composée de plusieurs services, actuellement exécutable avec Docker.

Votre objectif est de **préparer les images Docker puis de déployer l'application complète sur Kubernetes**.

Le projet est réalisé en groupe de **2 ou 3 étudiants**. Les séances de TD permettront de découvrir et mettre en pratique les principaux concepts, mais du travail personnel sera nécessaire entre les séances.

Le projet se terminera par une **soutenance avec démonstration**.

---

# Poser des questions

Vous pouvez, et vous devez, **poser des questions et demander des précisions** tout au long du projet.

L'enseignant a deux rôles :

- **le client** : il exprime le besoin et peut préciser le cahier des charges ;
- **l'expert technique** : il peut vous orienter, vous aider à diagnostiquer et à comprendre.

Comme avec un vrai client, ne pas poser de questions sur un point flou est un risque : **mieux vaut demander que supposer**.

---

# Prérequis

Nous considérons comme **acquis** :

- les **bases de Docker** : image, conteneur, Dockerfile, `docker run`, `docker compose` ;
- une **bonne connaissance de Git** : commit, branches, merge, remote, résolution de conflits ;
- l'utilisation de la **VM fournie par l'école** (ou de Linux si c'est votre système principal).

Si l'un de ces points vous pose problème, **signalez-le dès la première séance**.

---

# Votre environnement Kubernetes

Le choix de l'environnement vous appartient.

**Conseillé** : **minikube** sur la **VM fournie par l'école**, ou directement sur **Linux** si c'est votre système principal. C'est l'environnement utilisé et testé en cours : les commandes et exemples du cours le supposent.

**Vous restez libres** d'en choisir un autre :

- Kubernetes directement sur votre poste (Docker Desktop, kind, k3s...) ;
- un cluster dans le **cloud** ;
- un **service managé** d'un fournisseur cloud.

Dans tous les cas, **vous êtes autonomes** : installation, accès, coûts éventuels, nettoyage et dépannage de votre environnement sont **à votre charge**. Les explications du cours seront à adapter par vous.

Ce que cela implique :

- l'application doit **fonctionner dans l'environnement choisi**, et la **démonstration** de la soutenance se fait sur celui-ci : il doit être **opérationnel et accessible le jour J** ;
- la base de données reste **hors de Kubernetes** : elle doit être **joignable depuis votre cluster** (à vous de décider où elle tourne et comment les Pods la joignent) ;
- vos images doivent être **disponibles pour votre cluster** (images locales avec minikube, registry pour un cluster distant...) ;
- votre **README** doit permettre de **reproduire le déploiement dans votre environnement**, en précisant lequel.

---

# Travail attendu

## 1. Images Docker

Les images de l'application devront pouvoir être construites depuis le dépôt.

Vous devrez notamment :

- utiliser des **multi-stage builds** lorsque cela est pertinent ;
- éviter d'embarquer inutilement les outils et fichiers nécessaires uniquement au build ;
- utiliser correctement `.dockerignore` ;
- chercher à obtenir des images simples, légères et reproductibles.

Vous devrez être capables d'expliquer vos choix.

## 2. Déploiement Kubernetes

L'ensemble de l'application devra fonctionner sur Kubernetes.

Les différents composants devront utiliser des objets Kubernetes adaptés, notamment des **Deployments** plutôt que des Pods créés manuellement.

L'application devra utiliser un **Namespace dédié**.

La suppression d'un Pod applicatif ne devra pas empêcher Kubernetes de rétablir automatiquement l'état attendu.

## 3. Communication entre les services

Les différents composants devront pouvoir communiquer entre eux sans dépendre directement des adresses IP des Pods.

Vous devrez utiliser les mécanismes Kubernetes appropriés pour permettre la découverte des différents services.

L'application devra également être **accessible depuis l'extérieur du cluster**.

## 4. Configuration

La configuration propre à l'environnement ne devra pas nécessiter la reconstruction des images Docker.

Vous devrez utiliser les mécanismes Kubernetes appropriés pour séparer :

- la configuration applicative ;
- les informations sensibles.

Aucun mot de passe ou secret ne devra être intégré directement dans une image Docker.

## 5. Disponibilité

Les composants pour lesquels cela est pertinent devront disposer de **health checks** permettant à Kubernetes de déterminer leur état.

Vous devrez être capables d'expliquer le rôle des différentes probes utilisées.

Au moins un composant applicatif devra également pouvoir fonctionner avec **plusieurs replicas**.

## 6. Données

La base de données **reste en dehors de Kubernetes** : elle s'exécute dans un `docker-compose` séparé de l'application.

Vous devrez :

- sortir la base de données de la stack applicative et la faire fonctionner seule ;
- vous assurer que ses **données sont persistées** (volume) : la suppression et la recréation de son conteneur ne doit pas provoquer de perte de données ;
- connecter les composants déployés sur Kubernetes à cette base externe.

## 7. Observabilité

Vous devrez être capables de **montrer les logs** d'un composant de l'application et **au moins une métrique** (par exemple via l'endpoint `/metrics` d'un service, ou `kubectl top`).

Mettre en place une solution complète de centralisation des logs ou de monitoring (ELK, Prometheus, Grafana...) est **facultatif** : voir « Pour aller plus loin ».

---

# Livrable

Vous rendrez un dépôt Git contenant :

- le code fourni ;
- les Dockerfiles ;
- les manifests Kubernetes ;
- les éventuels scripts utilisés ;
- un `README.md` ;
- un dossier **`compte-rendu/`** (voir ci-dessous).

Le README devra permettre à une personne extérieure au groupe de :

1. construire les images ;
2. déployer l'application ;
3. accéder à l'application ;
4. vérifier rapidement son bon fonctionnement.

Un petit **schéma de l'architecture finale** est également attendu.

Un **modèle de README** est fourni dans le dépôt (`modeles/README-modele.md`) : vous pouvez vous en servir pour rédiger le vôtre.

## Branche évaluée

Pour la notation, **seule la branche `main` du dépôt est analysée** : le code, les manifests, le README, le compte rendu **et l'historique des commits**. La **date limite de rendu** est le **23/11/2026 à 13h30 précises**. Seul l'état de `main` à cet instant est analysé : tout ce qui n'y est pas à cette heure-là (commits poussés après, autres branches) **n'est pas pris en compte**.

## Compte rendu

Le dossier **`compte-rendu/`**, à la racine du dépôt, explique **ce qui a été fait et comment cela a été fait**. Les documents sont au format **Markdown (`.md`) ou AsciiDoc (`.adoc`)**.

Il doit contenir :

- **une section pour chacune des 7 parties du cahier des charges** (Images Docker, Déploiement Kubernetes, Communication, Configuration, Disponibilité, Données, Observabilité). Chaque section est soit **traitée** (ce qui a été fait, comment, pourquoi), soit explicitement indiquée comme **non traitée** ;
- **une section dédiée aux approfondissements** réalisés (« Pour aller plus loin »), si vous en avez fait. Ils doivent y être expliqués pour être pris en compte dans l'évaluation.

Un **modèle** est fourni dans le dépôt : le dossier `compte-rendu/` contient déjà un fichier par partie à compléter (vous pouvez le renommer, mais gardez la structure).

Le compte rendu complète le `README.md` : le README sert à **construire, déployer et vérifier**, le compte rendu sert à **expliquer et justifier**.

---

# Pour aller plus loin

Une fois le fonctionnement de base obtenu, vous êtes libres d'améliorer votre solution.

Quelques pistes :

- requests / limits CPU et mémoire ;
- Horizontal Pod Autoscaler ;
- stratégie de rolling update et rollback ;
- Helm ;
- NetworkPolicy ;
- RBAC ;
- amélioration de la sécurité des conteneurs ;
- monitoring et métriques ;
- centralisation des logs ;
- CI/CD ;
- automatisation du déploiement ;
- gestion avancée des secrets.

Ces fonctionnalités ne sont pas obligatoires. Elles pourront cependant être **valorisées lors de l'évaluation si elles sont pertinentes et maîtrisées**.

Vous pouvez également proposer d'autres améliorations.

---

# Soutenance

La soutenance comportera principalement :

- une présentation rapide de votre architecture ;
- une démonstration de l'application ;
- le **déploiement des 3 versions** de l'application (la version **initiale**, la version **évoluée** et la version **KO**), en montrant le code et les **logs du déploiement** ;
- une présentation de vos choix techniques ;
- l'affichage des **logs** et d'**au moins une métrique** d'un composant ;
- des questions sur Docker et Kubernetes.

**Préparez-vous en amont** : les images des trois versions doivent déjà être construites avant votre passage, et vous devez avoir testé le parcours complet (déployer, regarder les logs, vérifier que l'application fonctionne).

Avant de passer, vérifiez aussi que :

- votre **cluster Kubernetes** est démarré et accessible, et que la **base de données** (docker-compose séparé) est lancée, avec ses données ;
- vos images portent un **tag de version explicite** (pas de `latest`) ;
- vous savez **passer d'une version à l'autre**, mise à jour et rollback ;
- vos **commandes de déploiement sont prêtes** (script ou README) ;
- l'application est **accessible depuis le navigateur** ;
- tout est **poussé sur `main`** (testez un clone propre du dépôt) ;
- vous vous êtes **réparti la parole** et vous respectez le **temps de passage**.

Une manipulation ou un petit diagnostic pourra également vous être demandé directement sur votre environnement.

Chaque membre du groupe doit être capable d'expliquer le fonctionnement global de la solution.

---

# Évaluation

## Une note par groupe

La note est établie **pour le groupe**. Elle prend en compte :

- **le fonctionnement** : l'application fonctionne correctement sur **votre cluster Kubernetes** et respecte le cahier des charges ;
- **la qualité de la solution** : Dockerfiles, manifests, choix techniques pertinents ;
- **la reproductibilité** : le README et le schéma d'architecture permettent à une personne extérieure de construire, déployer, accéder et vérifier ;
- **le compte rendu** : chaque partie du cahier des charges est traitée, ou explicitement indiquée comme non traitée ;
- **l'historique du dépôt Git** : propre, avec des commits réguliers et des messages clairs, et **non réalisé par une seule personne** ;
- **la soutenance** : démonstration, manipulation ou diagnostic sur votre environnement, **questions / réponses bien gérées** ;
- **la compréhension des concepts et votre capacité à justifier vos choix**.

## Bonus : aller plus loin

Les **approfondissements** (voir « Pour aller plus loin ») sont **valorisés en bonus** s'ils sont **pertinents et maîtrisés**. Ils doivent être expliqués dans la section dédiée du compte rendu.

## Notes individuelles

La note du groupe est le point de départ, mais les **notes individuelles pourront être modifiées** si l'on constate que certaines personnes ont **nettement moins participé** que d'autres. Cela s'appuie notamment sur :

- **les contributions de chacun au dépôt** (les commits) ;
- **les réponses personnelles aux questions** lors de la soutenance.
