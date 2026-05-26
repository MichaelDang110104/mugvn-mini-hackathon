# ============================================
# Stage 1: Base image with pnpm
# ============================================
FROM node:22-alpine AS base

# Enable corepack for pnpm
RUN corepack enable && corepack prepare pnpm@latest --activate

WORKDIR /app

# ============================================
# Stage 2: Install dependencies (cached by lockfile)
# ============================================
FROM base AS deps

# Copy only dependency manifests first for cache optimisation
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./

# Install dependencies — this layer is cached until lockfile changes
RUN pnpm install --frozen-lockfile --prefer-offline

# ============================================
# Stage 3: Build the Next.js application
# ============================================
FROM base AS builder

WORKDIR /app

# Copy cached node_modules from deps stage
COPY --from=deps /app/node_modules ./node_modules

# Copy all source files
COPY . .

# Set the backend URL at build time (can be overridden via docker-compose)
ARG NEXT_PUBLIC_API_BASE_URL=http://localhost:9000
ENV NEXT_PUBLIC_API_BASE_URL=${NEXT_PUBLIC_API_BASE_URL}

# Build with standalone output for minimal production image
ENV NEXT_TELEMETRY_DISABLED=1
RUN pnpm build

# ============================================
# Stage 4: Minimal production runner
# ============================================
FROM node:22-alpine AS runner

WORKDIR /app

ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

# Create non-root user for security
RUN addgroup --system --gid 1001 nodejs && \
    adduser --system --uid 1001 nextjs

# Copy only the necessary production artifacts
COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs

EXPOSE 3000

ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

# Use the standalone server (no node_modules needed at runtime)
CMD ["node", "server.js"]
