FROM node:22.11.0-alpine
WORKDIR /app
COPY package.json package-lock.json tsconfig.json ./
COPY src ./src
RUN npm install && npm run build && npm prune --omit=dev && rm -rf src tsconfig.json
CMD ["node", "dist/Server.js"]
