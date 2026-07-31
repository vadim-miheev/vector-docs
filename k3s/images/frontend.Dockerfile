FROM node:22.18 AS build
WORKDIR /app
COPY frontend/react-app/package.json frontend/react-app/package-lock.json ./
RUN npm ci
COPY frontend/react-app/ .
ARG REACT_APP_DEMO_USER_ID=1
ENV REACT_APP_DEMO_USER_ID=${REACT_APP_DEMO_USER_ID}
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/build /usr/share/nginx/html
COPY frontend/nginx/default.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
