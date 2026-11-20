# === build ===
# image Maven + JDK 21
FROM maven:3.9-eclipse-temurin-21 AS build 
# on se place dans le dossier /app
WORKDIR /app  
# on copie seulement pom.xml et on télécharge les dépendances (couche mise en cache)
COPY pom.xml .   
RUN mvn -B dependency:go-offline
# on copie le code
COPY src ./src
# on compile (.jar)
RUN mvn clean package -DskipTests 


# === run ===
# image contenant le JRE (permet de lancer le java sans compiler)
FROM eclipse-temurin:21-jre  
WORKDIR /app
# on crée un utilisateur sans droits spéciaux
RUN groupadd --system app && useradd --system --gid app --no-create-home app
# on cherche le .jar du build
COPY --from=build /app/target/*.jar app.jar 
USER app
EXPOSE 8080
# commande lancée au démarrage
CMD ["java", "-jar", "app.jar"]   

