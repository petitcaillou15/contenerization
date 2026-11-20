FROM nginx:1.27-alpine
# suppression de 2 lignes qui installe des outils inutiles pour faire tourner nginx
COPY default.conf.template /etc/nginx/templates/default.conf.template
ENV USERS_UPSTREAM=users:8080 \
    POSTS_UPSTREAM=posts:8080 \
    NOTIFICATIONS_UPSTREAM=notifications:8080 \
    CORS_ALLOW_ORIGIN=*
EXPOSE 80
