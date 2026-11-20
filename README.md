# TD Kubernetes — support étudiant

## Membres du groupe

À remplir dans votre premier commit.

| Prénom | Nom  | Email                     | ID GitLab    |
|--------|------|---------------------------|--------------|
|Antoine |Horion| antoine.horion17@gmail.com|horion.antoine|
|Maxime  |LA    |la.maxime2004@gmail.com    |la.maxime     |
|Hakim   |Fayala|hfayala53@gmail.com        |hakim.fayala  |



Ce dépôt contient tout ce qu'il faut pour démarrer le projet :

- `projet/` : l'application (code fourni) et ses Dockerfiles à améliorer
- `consignes/attendu.md` : le cahier des charges et les livrables
- `compte-rendu/` : modèle du compte rendu à compléter (un fichier par partie du cahier des charges)
- `modeles/README-modele.md` : modèle du README final à rédiger
- `cours/` : les supports de cours en AsciiDoc, avec table des matières (`cours/seance1.adoc`, lisible directement sur GitLab)

## Démarrage

1. Clonez ce dépôt.
2. Créez un dépôt **vide** pour votre groupe sur le GitLab de l'école, et ajoutez vos camarades ainsi que l'enseignant.
3. **Ajoutez votre dépôt comme `origin`.** Après le clone, `origin` pointe vers le dépôt du cours : renommez-le en `upstream` (vous y récupérerez les nouvelles versions de l'application), puis ajoutez **votre** dépôt comme nouvel `origin` et poussez :

```bash
git remote rename origin upstream
git remote add origin <url-de-votre-depot>
git push -u origin main
```

`origin` est **votre** dépôt de groupe (c'est lui qui est évalué), `upstream` est le dépôt du cours.

4. Remplissez le tableau des membres ci-dessus, puis faites votre premier commit et poussez.
5. **Seule la branche `main` de votre dépôt est évaluée** (code, documents et historique des commits), à la **date limite du 23/11/2026 à 13h30 précises**.
6. Lancez l'application avec Docker pour voir ce qu'elle fait : voir `projet/README.md`.


## Environnement Kubernetes

Il est **conseillé** d'utiliser **minikube** sur la **VM de l'école** (ou directement sur Linux). Mais vous êtes **libres** d'utiliser un autre environnement : Kubernetes sur votre poste, un cluster cloud ou un service managé.
Dans ce cas, **vous êtes autonomes** : installation, accès, coûts, dépannage. Voir `consignes/attendu.md`.

## Nouvelles versions de l'application

Des **nouvelles versions de l'application** vous seront mises à disposition en cours de projet,
sur des branches du dépôt d'origine (`upstream`). Pour en récupérer une, par exemple `v2` :

```bash
git fetch upstream
git merge upstream/v2
```

Ne vous attendez pas à des conflits sur vos Dockerfiles et vos manifests : ces versions ne
modifient que le code de l'application.
