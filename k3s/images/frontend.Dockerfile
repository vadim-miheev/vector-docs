# Frontend config (REACT_APP_*) is runtime-only: the image is environment-agnostic.
# Per-environment values are injected at runtime via nginx serving /config.js,
# populated from the vector-docs-config ConfigMap (k3s/configmap.yaml).
FROM node:22.18 AS build
WORKDIR /app
COPY frontend/react-app/package.json frontend/react-app/package-lock.json ./
RUN npm ci
COPY frontend/react-app/ .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/build /usr/share/nginx/html
COPY frontend/nginx/default.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
