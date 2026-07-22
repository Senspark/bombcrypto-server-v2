FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json tsconfig.json ./
RUN npm ci
COPY src ./src
RUN npm run build

FROM node:24-alpine
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --omit=dev
COPY --from=build /app/dist ./dist
# Build stamp passed by the deploy script (compose build-arg). Near the end so it never busts the npm/build
# cache — only this cheap layer rebuilds when the version changes, which it does on every deploy.
ARG APP_VERSION=dev
ENV APP_VERSION=$APP_VERSION
CMD ["node", "dist/Server.js"]
