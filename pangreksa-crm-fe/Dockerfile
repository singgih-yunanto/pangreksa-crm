# Build stage: Node 20 on Alpine
FROM node:20-alpine AS builder
WORKDIR /app
# Install deps as a separate cached layer
COPY package.json package-lock.json ./
RUN npm ci
# NEXT_PUBLIC_* vars must be set at build time (baked into the bundle)
ARG NEXT_PUBLIC_API_BASE_URL=""
ENV NEXT_PUBLIC_API_BASE_URL=${NEXT_PUBLIC_API_BASE_URL}
COPY . .
RUN npm run build

# Runtime stage
FROM node:20-alpine
WORKDIR /app
COPY --from=builder /app/.next ./.next
COPY --from=builder /app/public ./public
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/package.json ./package.json
EXPOSE 3000
CMD ["node_modules/.bin/next", "start"]
