#!/usr/bin/env bash
set -euo pipefail

# =============================================================================
# Push vector-docs images to Docker Hub
# =============================================================================
# Prerequisites:
#   - Images are built (run ./k3s/scripts/build-images.sh first)
#   - Docker is available
#   - Logged in to Docker Hub (docker login)
#
# Usage:
#   ./k3s/scripts/push-images.sh               # push all images
#   ./k3s/scripts/push-images.sh gateway       # push a single image (by service name)
#
# Images are pushed as <DOCKERHUB_USER>/vector-docs-<service>:<tag>.
# Docker Hub requires flat repository names (no slashes after the username),
# so the prefix is added via a hyphen, not a slash.
# =============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Docker Hub username; override if needed, e.g.:
#   DOCKERHUB_USER=someone-else ./k3s/scripts/push-images.sh
DOCKERHUB_USER="${DOCKERHUB_USER:-vadimmiheev}"

IMAGE_PREFIX="vector-docs"
TAG="${TAG:-latest}"

if [ -z "${DOCKERHUB_USER}" ]; then
    echo "ERROR: DOCKERHUB_USER is empty. Set it or edit the script." >&2
    exit 1
fi

# All services that can be pushed
ALL_SERVICES=(
    gateway
    search-service
    answer-generator
    notification-service
    storage-service
    document-processor
    frontend
    flyway
)

# Determine which services to push
if [ $# -gt 0 ]; then
    SERVICES=("$@")
else
    SERVICES=("${ALL_SERVICES[@]}")
fi

echo "============================================"
echo " Pushing images to Docker Hub"
echo " Target: ${DOCKERHUB_USER}/${IMAGE_PREFIX}-<service>:${TAG}"
echo " Services: ${SERVICES[*]}"
echo "============================================"
echo ""

# Check Docker Hub login
if ! docker info 2>/dev/null | grep -q "Username:"; then
    echo "WARNING: Docker Hub login not detected. Push may fail."
    echo "  Run:  docker login"
    echo ""
fi

pushed=0
for service in "${SERVICES[@]}"; do
    source_image="${IMAGE_PREFIX}/${service}:${TAG}"
    target_image="${DOCKERHUB_USER}/${IMAGE_PREFIX}-${service}:${TAG}"

    # Verify the Docker image exists locally
    if ! docker image inspect "${source_image}" &>/dev/null; then
        echo "WARNING: Docker image '${source_image}' not found locally, skipping..."
        continue
    fi

    echo "--- ${target_image} ---"

    docker tag "${source_image}" "${target_image}"
    docker push "${target_image}"

    echo "✓ ${target_image} pushed"
    echo ""
    pushed=$((pushed + 1))
done

echo "============================================"
echo " Done! ${pushed} image(s) pushed"
echo "============================================"
echo ""
echo "Update k3s manifests to reference Docker Hub:"
echo "  sed -i 's|image: vector-docs/|image: ${DOCKERHUB_USER}/vector-docs-|g' k3s/*.yaml"
echo ""
echo "Then deploy:"
echo "  kubectl apply -k k3s/"