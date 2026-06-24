# ─────────────────────────────────────────────────────────────────────────────
# Stage 1 — Build: Node.js 20 Alpine
# ─────────────────────────────────────────────────────────────────────────────
FROM node:20-alpine AS builder

WORKDIR /app

# Copy dependency manifests first for caching
COPY package.json package-lock.json ./

# Install clean production and build dependencies
RUN npm ci

# Copy configuration and source files
COPY tsconfig.json tsconfig.app.json tsconfig.node.json vite.config.ts tailwind.config.js postcss.config.js eslint.config.js index.html ./
COPY public public
COPY src src

# Inject API Base URL during build-time (empty/fallback handles runtime)
ARG VITE_API_BASE_URL
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

# Build the React production SPA
RUN npm run build

# ─────────────────────────────────────────────────────────────────────────────
# Stage 2 — Runtime: Nginx Alpine
# ─────────────────────────────────────────────────────────────────────────────
FROM nginx:alpine AS runtime

# Copy SPA custom Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy static assets from build stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Expose HTTP port
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
