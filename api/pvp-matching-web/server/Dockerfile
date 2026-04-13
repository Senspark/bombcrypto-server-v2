FROM node:20.11.1-alpine as build
WORKDIR /app
COPY package.json ./
COPY package-lock.json ./
COPY tsconfig.json ./
COPY src ./src
#COPY node_modules ./node_modules
RUN npm install
RUN npm run build

FROM node:20.11.1-alpine
WORKDIR /app
COPY package.json ./
COPY package-lock.json ./
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/dist ./dist
CMD ["node", "dist/Server.js"]
