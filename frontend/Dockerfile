# syntax=docker/dockerfile:1

# Compila o bundle do Vite numa fase Node e serve o resultado num nginx de produção.
#
# O `COPY package*.json` antes do resto mantém a camada de instalação estável: mudar uma
# linha de .vue não reinstala dependência nenhuma, e o cache mount de /root/.npm segura o
# offline rebuild. O bundle é servido com caminho relativo (`/api` vem do default em
# src/services/api.js), por isso não precisa de VITE_API_BASE_URL em build arg — quem resolve
# o backend é o proxy do nginx.conf, e o navegador vê uma origem só.
#
# A imagem final não tem Node nem node_modules: só dist estático e a configuração do nginx.
# O healthcheck do docker-compose usa o wget do busybox que já vem na imagem.

FROM node:26-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund

COPY . .
RUN npm run build

FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
