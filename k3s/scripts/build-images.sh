#!/usr/bin/env bash
set -euo pipefail

# =============================================================================
# Build images for vector-docs k3s cluster
# =============================================================================
# Prerequisites:
#   - JARs are built (`make dev-build`)
#   - Docker is available
#
# After building, load images into the cluster via one of:
#   Option A (registry):  docker push <registry>/vector-docs/<service>:latest
#   Option B (direct):    docker save ... | ssh node sudo ctr images import -
#   Option C (local):     build directly on control-plane node
# =============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"

IMAGE_PREFIX="vector-docs"
TAG="latest"

# Override with registry prefix if needed, e.g.:
#   REGISTRY=registry.example.com ./k3s/scripts/build-images.sh
REGISTRY="${REGISTRY:-}"

echo "============================================"
echo " Building vector-docs Docker images"
echo "============================================"

build_image() {
    local service_name="$1"
    local dockerfile="$2"
    local context="$3"
    local jar_path="${4:-}"
    local tag="${REGISTRY:+${REGISTRY}/}${IMAGE_PREFIX}/${service_name}:${TAG}"

    echo ""
    echo "--- ${tag} ---"

    if [ -n "${jar_path}" ]; then
        local full_path="${context}/${jar_path}"
        if [ ! -f "${full_path}" ]; then
            echo "WARNING: JAR not found at ${full_path}, skipping..."
            return 1
        fi
        docker build \
            -f "${dockerfile}" \
            -t "${tag}" \
            --build-arg "JAR_FILE=${jar_path}" \
            "${context}"
    else
        docker build \
            -f "${dockerfile}" \
            -t "${tag}" \
            "${context}"
    fi

    echo "✓ ${tag} built"
    echo "  To push:  docker push ${tag}"
}

# ---- Java services (generic Dockerfile) ----
for service in gateway search-service answer-generator notification-service storage-service; do
    case "${service}" in
        gateway)                src="${PROJECT_DIR}/gateway" ;;
        search-service)         src="${PROJECT_DIR}/services/search-service" ;;
        answer-generator)       src="${PROJECT_DIR}/services/answer-generator" ;;
        notification-service)   src="${PROJECT_DIR}/services/notification-service" ;;
        storage-service)        src="${PROJECT_DIR}/services/storage-service" ;;
    esac
    build_image \
        "${service}" \
        "${SCRIPT_DIR}/../images/service.Dockerfile" \
        "${src}" \
        "build/libs/${service}.jar" || true
done

# ---- document-processor (tesseract) ----
build_image \
    "document-processor" \
    "${SCRIPT_DIR}/../images/document-processor.Dockerfile" \
    "${PROJECT_DIR}/services/document-processor" \
    "build/libs/document-processor.jar" || true

# ---- frontend (multi-stage) ----
build_image \
    "frontend" \
    "${SCRIPT_DIR}/../images/frontend.Dockerfile" \
    "${PROJECT_DIR}" || true

# ---- flyway (with migrations bundled) ----
build_image \
    "flyway" \
    "${SCRIPT_DIR}/../images/flyway.Dockerfile" \
    "${PROJECT_DIR}" || true

echo ""
echo "============================================"
echo " All images built!"
echo "============================================"
echo ""
echo "Load into the cluster:"
echo "  Option 1 — push to registry:"
echo "    REGISTRY=myregistry.example.com ./k3s/scripts/build-images.sh"
echo "    kubectl set image -n vector-docs deployment/gateway ..."
echo ""
echo "  Option 2 — save and import on node:"
echo "    docker save ${IMAGE_PREFIX}/gateway:${TAG} | bzip2 | \\"
echo "      ssh <node> sudo k3s ctr images import -"
echo ""
echo "  Option 3 — build directly on each node:"
echo "    Copy project to node, run ./k3s/scripts/build-images.sh"
echo ""
echo "Deploy:"
echo "    kubectl apply -k k3s/"
