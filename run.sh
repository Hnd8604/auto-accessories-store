#!/usr/bin/env bash
# ==============================================================
# Starts the Auto Accessories Store project on Linux/macOS
# (bash port of run.ps1).
#
#   1. PostgreSQL + Redis via Docker Compose (--profile infra)
#   2. Spring Boot backend   -> http://localhost:8080/api/v1
#   3. Vite frontend         -> http://localhost:3000
#
# Backend and frontend run in this terminal with prefixed output.
# Press Ctrl+C to stop both. Docker infra keeps running until --stop.
#
# Usage:
#   ./run.sh                          Start everything
#   ./run.sh --only infra,backend     Start only selected parts
#   ./run.sh --only frontend --skip-install
#   ./run.sh --stop                   Stop Docker infrastructure
# ==============================================================

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$ROOT/backend"
FRONTEND_DIR="$ROOT/frontend"
BACKEND_LOGS="$ROOT/logs/backend"
FRONTEND_LOGS="$ROOT/logs/frontend"
DEV_ENV_FILE="$ROOT/.env.dev"
DEV_ENV_EXAMPLE="$ROOT/.env.dev.example"
RUN_STAMP="$(date +%Y-%m-%d_%H-%M-%S)"

ONLY="infra,backend,frontend"
SKIP_INSTALL=0
STOP=0

# ---------------------------- output helpers ----------------------------
if [[ -t 1 ]]; then
    C_CYAN=$'\e[36m'; C_GREEN=$'\e[32m'; C_YELLOW=$'\e[33m'; C_RED=$'\e[31m'
    C_MAGENTA=$'\e[35m'; C_GRAY=$'\e[90m'; C_RESET=$'\e[0m'
else
    C_CYAN=''; C_GREEN=''; C_YELLOW=''; C_RED=''; C_MAGENTA=''; C_GRAY=''; C_RESET=''
fi
step() { printf '\n%s==> %s%s\n' "$C_CYAN" "$1" "$C_RESET"; }
ok()   { printf '    %s%s%s\n' "$C_GREEN" "$1" "$C_RESET"; }
warn() { printf '    %s%s%s\n' "$C_YELLOW" "$1" "$C_RESET"; }
die()  { printf '%sERROR: %s%s\n' "$C_RED" "$1" "$C_RESET" >&2; exit 1; }

usage() { sed -n '3,18p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; }

# ---------------------------- arguments ----------------------------
while [[ $# -gt 0 ]]; do
    case "$1" in
        --only)          ONLY="${2:-}"; shift 2 ;;
        --only=*)        ONLY="${1#*=}"; shift ;;
        --skip-install)  SKIP_INSTALL=1; shift ;;
        --stop)          STOP=1; shift ;;
        -h|--help)       usage; exit 0 ;;
        *)               usage; die "Unknown option: $1" ;;
    esac
done

IFS=',' read -r -a PARTS <<< "$ONLY"
for p in "${PARTS[@]}"; do
    [[ "$p" =~ ^(infra|backend|frontend)$ ]] || die "Invalid --only value '$p' (use infra, backend, frontend)"
done
wants() { [[ ",$ONLY," == *",$1,"* ]]; }
has()   { command -v "$1" >/dev/null 2>&1; }

mkdir -p "$BACKEND_LOGS" "$FRONTEND_LOGS"

# All dev compose commands run from ROOT with --env-file pointing at .env.dev.
# Without it Compose looks for a bare `.env`, and POSTGRES_* end up empty.
dev_compose() {
    local env_args=()
    [[ -f "$DEV_ENV_FILE" ]] && env_args=(--env-file "$DEV_ENV_FILE")
    (cd "$ROOT" && docker compose "${env_args[@]}" "$@")
}

# Spring Boot does not read .env.dev on its own, so load it into this shell's
# environment. Parsed line by line (not `source`) so values with spaces such as
# MAIL_FROM_NAME=Auto Store work without quoting.
import_dev_env() {
    if [[ ! -f "$DEV_ENV_FILE" ]]; then
        warn "No $DEV_ENV_FILE found - backend will be missing environment variables."
        return
    fi
    local line name value
    while IFS= read -r line || [[ -n "$line" ]]; do
        line="${line%$'\r'}"
        [[ "$line" =~ ^[[:space:]]*([A-Za-z_][A-Za-z0-9_]*)=(.*)$ ]] || continue
        name="${BASH_REMATCH[1]}"
        value="${BASH_REMATCH[2]}"
        # Strip trailing comment (after 2+ spaces), then trim whitespace.
        value="$(sed -E 's/[[:space:]]{2,}#.*$//; s/^[[:space:]]+//; s/[[:space:]]+$//' <<< "$value")"
        export "$name=$value"
    done < "$DEV_ENV_FILE"
    # Backend runs outside Docker, so point it at the ports Compose publishes.
    export DB_HOST=localhost REDIS_HOST=localhost
    ok "Loaded environment variables from $DEV_ENV_FILE"
}

# ---------------------------- stop mode ----------------------------
if [[ $STOP -eq 1 ]]; then
    step "Stopping Docker infrastructure..."
    dev_compose --profile infra down
    ok "Infrastructure stopped."
    exit 0
fi

# ---------------------------- prerequisites ----------------------------
step "Checking prerequisites..."
if wants infra; then
    has docker || die "Docker is not installed or not on PATH. Install Docker, or run with --only backend,frontend."
    docker compose version >/dev/null 2>&1 || die "Docker Compose v2 plugin not found ('docker compose')."
    docker info >/dev/null 2>&1 || die "Cannot talk to the Docker daemon. Is it running, and is your user in the 'docker' group?"
fi
if wants backend; then
    has java || die "Java is not installed or not on PATH. Install a JDK (e.g. sudo apt install openjdk-21-jdk)."
    [[ -f "$BACKEND_DIR/mvnw" ]] || die "Maven wrapper not found at $BACKEND_DIR/mvnw"
    [[ -x "$BACKEND_DIR/mvnw" ]] || chmod +x "$BACKEND_DIR/mvnw"
fi
if wants frontend; then
    has node || die "Node.js is not installed or not on PATH. Install Node.js 18+ from https://nodejs.org"
    has npm  || die "npm is not installed or not on PATH."
fi
ok "Prerequisites OK."

# ---------------------------- 1. infrastructure ----------------------------
if wants infra; then
    step "Starting infrastructure (PostgreSQL, Redis)..."
    if [[ ! -f "$DEV_ENV_FILE" ]]; then
        if [[ -f "$DEV_ENV_EXAMPLE" ]]; then
            cp "$DEV_ENV_EXAMPLE" "$DEV_ENV_FILE"
            warn "Created .env.dev from .env.dev.example - review the passwords inside it."
        else
            warn "No .env.dev found and no .env.dev.example to copy. Docker Compose may fail."
        fi
    fi
    # --wait blocks until the healthchecks pass, so the backend can connect right away.
    dev_compose --profile infra up -d --wait || die "docker compose failed to start infrastructure."
    ok "Infrastructure containers are up: postgres:5432, redis:6379."
fi

# Nothing long-running requested -> done.
if ! wants backend && ! wants frontend; then
    step "Done."
    printf '    %sStop infrastructure with:  ./run.sh --stop%s\n' "$C_GRAY" "$C_RESET"
    exit 0
fi

# ---------------------------- process management ----------------------------
PIDS=()
cleanup() {
    trap - INT TERM EXIT
    if [[ ${#PIDS[@]} -gt 0 ]]; then
        step "Stopping backend/frontend..."
        for pid in "${PIDS[@]}"; do
            # Kill the whole process group so mvn/java and node children die too.
            kill -TERM -- "-$pid" 2>/dev/null || kill -TERM "$pid" 2>/dev/null || true
        done
        wait 2>/dev/null || true
        ok "Stopped."
        if wants infra; then
            printf '    %sInfrastructure is still running. Stop it with:  ./run.sh --stop%s\n' "$C_GRAY" "$C_RESET"
        fi
    fi
}
trap cleanup INT TERM EXIT

# run_service NAME COLOR DIR LOGFILE CMD...
# Runs CMD in its own process group, prefixes each line with [NAME] and tees to LOGFILE.
run_service() {
    local name="$1" color="$2" dir="$3" log="$4"; shift 4
    setsid bash -c '
        cd "$1" || exit 1
        name="$2"; color="$3"; reset="$4"; log="$5"; shift 5
        "$@" 2>&1 | tee -a "$log" | while IFS= read -r l; do
            printf "%s[%s]%s %s\n" "$color" "$name" "$reset" "$l"
        done
    ' _ "$dir" "$name" "$color" "$C_RESET" "$log" "$@" &
    PIDS+=("$!")
}

# ---------------------------- 2. backend ----------------------------
if wants backend; then
    step "Launching backend (Spring Boot)..."
    import_dev_env
    backend_log="$BACKEND_LOGS/console-$RUN_STAMP.log"
    run_service backend "$C_MAGENTA" "$BACKEND_DIR" "$backend_log" ./mvnw spring-boot:run
    ok "Backend starting at http://localhost:8080/api/v1 (Swagger: /swagger-ui.html)"
    ok "Backend console log: $backend_log"
    ok "Backend app log:     $BACKEND_LOGS/backend.log"
fi

# ---------------------------- 3. frontend ----------------------------
if wants frontend; then
    step "Launching frontend (Vite)..."
    # Vite reads .env.dev from the project root itself (see loadEnv in frontend/vite.config.ts).
    frontend_log="$FRONTEND_LOGS/console-$RUN_STAMP.log"
    if [[ $SKIP_INSTALL -eq 1 ]]; then
        run_service frontend "$C_CYAN" "$FRONTEND_DIR" "$frontend_log" npm run dev
    else
        run_service frontend "$C_CYAN" "$FRONTEND_DIR" "$frontend_log" sh -c 'npm install && npm run dev'
    fi
    ok "Frontend starting at http://localhost:3000"
    ok "Frontend console log: $frontend_log"
fi

step "Running. Press Ctrl+C to stop backend/frontend."

# Exit (and clean up the other service) as soon as either one dies.
wait -n "${PIDS[@]}" || true
