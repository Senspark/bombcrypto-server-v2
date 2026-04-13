# PVP Matching Server

This directory contains the backend server for setting up and testing Bombcrypto PVP Tournaments or play a pvp match on a specific server.

## Overview
- **Purpose:** Provides RESTful APIs and services for PVP match scheduling, test matches, and related operations.
- **Tech Stack:** Node.js, TypeScript, Express.js
- **Structure:**
  - `src/` - Main source code (controllers, routers, services, utils)
  - `package.json` - Project dependencies and scripts
  - `Dockerfile` - Containerization setup
  - `tsconfig.json` - TypeScript configuration

## How to Run

### 1. Install dependencies
```bash
npm install
```

### 2. Build the project
```bash
npm run build
```

### 3. Start the server
```bash
npm start
```

### 4. Run with Docker
```bash
docker build -t pvp-matching-server .
docker run -p 3000:3000 pvp-matching-server
```

## Notes
- Configuration is managed via environment variables (see `.env.example` or `EnvConfig.ts`).
- Main entry point: `src/Server.ts`
- API routes are defined in `src/Routes.ts` and `src/routers/`.

---
