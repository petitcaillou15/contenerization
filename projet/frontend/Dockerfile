# === build ===
# image avec Node.js (pour compiler l'appli Angular)
FROM node:22 AS build
# on se place dans le dossier /app
WORKDIR /app
# on copie et installe seulement les fichiers de dépendances (couche mise en cache)
COPY package.json package-lock.json ./
RUN npm ci
# on copie le code et on compile 
COPY . . 
RUN npm run build


# === run ===
# image nginx légère 
FROM nginx:alpine
# valeur par défaut de l'adresse de la gateway
ENV GATEWAY_UPSTREAM=gateway:80
# config nginx : au démarrage, nginx remplace ${GATEWAY_UPSTREAM} par sa valeur
COPY default.conf.template /etc/nginx/templates/default.conf.template
# on récupère seulement les fichiers du site depuis l'étape build
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
