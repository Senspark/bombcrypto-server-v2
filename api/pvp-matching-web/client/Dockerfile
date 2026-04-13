FROM node:22.11.0-alpine AS build
WORKDIR /app
COPY package.json .
COPY package-lock.json .
COPY tsconfig.json .
COPY src ./src
COPY public ./public
COPY .env .env
RUN npm install
RUN npm run build

FROM node:22.11.0-alpine
WORKDIR /app
COPY package.json .
COPY package-lock.json .
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/build ./build
RUN npm install -g serve
CMD ["serve", "-s", "build", "-l", "80", "-L"]