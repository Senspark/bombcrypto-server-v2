FROM node:20.11.1-alpine AS build
WORKDIR /app
COPY package.json ./
COPY package-lock.json ./
COPY tsconfig.json ./
COPY src ./src
RUN npm install
RUN npm run build

FROM node:20.11.1-alpine
WORKDIR /app
COPY package.json ./
COPY package-lock.json ./
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/dist .
COPY ./data ./data
CMD ["node", "Server.js"]