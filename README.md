# Despliegue desafío Pokemon en Google Cloud

Despliegue de la solución Redis (para cache de la aplicación), Backend (Spring Boot) y Frontend (ReactJS) en Google Cloud, usando Cloud Run y una VM para Redis.

---

## 1. Creación del proyecto en Google Cloud

Lo primero que hice fue crear un proyecto en Google Cloud llamado `desafio-pokemon-vsalse`. Desde la consola de Google Cloud, lo seleccioné y luego lo configuré en la CLI con:

```sh
gcloud config set project desafio-pokemon-vsalse
```

---

## 2. Habilitación de APIs necesarias

Para poder usar Cloud Run, Container Registry, Redis y VPC Access, habilité las siguientes APIs:

```sh
gcloud services enable run.googleapis.com
gcloud services enable containerregistry.googleapis.com
gcloud services enable redis.googleapis.com
gcloud services enable vpcaccess.googleapis.com
```

---

## 3. Despliegue de Redis en una VM

Quise tener control total sobre Redis, así que decidí levantarlo en una VM propia (Compute Engine):

### 3.1. Creación de la VM

```sh
gcloud compute instances create redis-demo \
  --zone=us-central1-a \
  --machine-type=f1-micro \
  --image-family=debian-11 \
  --image-project=debian-cloud \
  --tags=redis-server \
  --scopes=default
```

### 3.2. Configuración de firewall para Cloud Run

Abrí el puerto 6379 para mi IP local para poder acceder por ssh y poder instalar REDIS:

```sh
gcloud compute firewall-rules create allow-redis \
  --allow=tcp:6379 \
  --target-tags=redis-server \
  --source-ranges=45.224.141.106/32
```

Abrí el puerto 6379 para el rango de IPs de Cloud Run, para que el back pueda conectarse al REDIS:

```sh
gcloud compute firewall-rules create allow-redis-cloudrun \
  --allow=tcp:6379 \
  --target-tags=redis-server \
  --source-ranges=35.235.240.0/20 \
  --description="Permitir acceso desde Cloud Run a Redis" \
  --network=default
```

### 3.3. Instalación y configuración de Redis

Me conecté por SSH a la VM y ejecuté:

```sh
sudo apt-get update
sudo apt-get install redis-server -y
sudo sed -i 's/^bind .*/bind 0.0.0.0/' /etc/redis/redis.conf
sudo systemctl restart redis-server
exit
```

### 3.4. Obtención de la IP privada de la VM

```sh
gcloud compute instances describe redis-demo \
  --zone=us-central1-a \
  --format="get(networkInterfaces[0].networkIP)"
```

### 3.5. Creación del VPC Serverless Connector

Para que Cloud Run pudiera acceder a Redis por IP privada, creé el conector:

```sh
gcloud compute networks vpc-access connectors create serverless-connector \
  --region=us-central1 \
  --network=default \
  --range=10.8.0.0/28
```

---

## 4. Preparación de los Dockerfile

### 4.1. Dockerfile del Backend

```dockerfile
# Dockerfile para backend
FROM eclipse-temurin:17-jdk as build
WORKDIR /app
COPY pokemon-api .
RUN chmod +x gradlew
RUN ./gradlew build -x test

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Djava.net.preferIPv4Stack=true", "-jar", "app.jar"]
```

### 4.2. Dockerfile del Frontend

```dockerfile
# Dockerfile para frontend
FROM node:20-alpine as build
WORKDIR /app
COPY pokemon-front .
RUN npm install

# Set default API URL for build (will be overridden by environment variable)
ARG REACT_APP_API_URL=http://localhost:8080
ENV REACT_APP_API_URL=$REACT_APP_API_URL

RUN npm run build

FROM nginx:alpine
COPY --from=build /app/build /usr/share/nginx/html

# Create nginx configuration for port 8080
RUN echo 'server { \
    listen 8080; \
    server_name _; \
    root /usr/share/nginx/html; \
    index index.html index.htm; \
    location / { \
        try_files $uri $uri/ /index.html; \
    } \
    location /static/ { \
        expires 1y; \
        add_header Cache-Control "public, immutable"; \
    } \
}' > /etc/nginx/conf.d/default.conf

EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]
```

---

## 5. Despliegues

### Backend

```sh
docker build -t gcr.io/desafio-pokemon-vsalse/pokemon-backend:latest -f pokemon-dist/Dockerfile.backend .

docker push gcr.io/desafio-pokemon-vsalse/pokemon-backend:latest

gcloud run deploy pokemon-backend \
  --image gcr.io/desafio-pokemon-vsalse/pokemon-backend:latest \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --vpc-connector=serverless-connector \
  --vpc-egress=private-ranges-only \
  --set-env-vars REDIS_HOST=10.128.0.2,REDIS_PORT=6379
```

### Frontend

```sh
docker build -f pokemon-dist/Dockerfile.frontend \
  --build-arg REACT_APP_API_URL=https://pokemon-backend-422670589510.us-central1.run.app \
  -t gcr.io/desafio-pokemon-vsalse/pokemon-frontend:latest .

docker push gcr.io/desafio-pokemon-vsalse/pokemon-frontend:latest

gcloud run deploy pokemon-frontend \
  --image gcr.io/desafio-pokemon-vsalse/pokemon-frontend:latest \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated
```

---

## URLs

- **Frontend:**  
  https://pokemon-frontend-422670589510.us-central1.run.app
- **Backend:**  
  https://pokemon-backend-422670589510.us-central1.run.app
- **Swagger:**  
  https://pokemon-backend-422670589510.us-central1.run.app/swagger-ui.html 