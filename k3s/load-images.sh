#!/usr/bin/env bash
set -euo pipefail

# =============================================================================
# Load Docker images into k3s cluster (control-plane node)
# =============================================================================
# Prerequisites:
#   - Images are built (run ./k3s/build-images.sh first)
#   - Run this ON the k3s control-plane node (or node where ctr is available)
#   - sudo access for k3s ctr
#
# Usage:
#   ./k3s/load-images.sh              # load all images
#   ./k3s/load-images.sh gateway      # load a single image (by service name)
# =============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

IMAGE_PREFIX="vector-docs"
TAG="latest"

# All services that can be loaded
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

# Determine which services to load
if [ $# -gt 0 ]; then
    SERVICES=()
    for arg in "$@"; do
        SERVICES+=("${arg}")
    done
else
    SERVICES=("${ALL_SERVICES[@]}")
fi

echo "============================================"
echo " Loading images into k3s: ${SERVICES[*]}"
echo "============================================"
echo ""

K3S_CTR="sudo k3s ctr images import"

for service in "${SERVICES[@]}"; do
    image="${IMAGE_PREFIX}/${service}:${TAG}"

    # Verify the Docker image exists locally
    if ! docker image inspect "${image}" &>/dev/null; then
        echo "WARNING: Docker image '${image}' not found locally, skipping..."
        continue
    fi

    echo "--- ${image} ---"

    # Check if image is already in k3s ctr
    if sudo k3s ctr images ls -q 2>/dev/null | grep -qF "${image}"; then
        echo "  Image already present in k3s, skipping..."
        echo ""
        continue
    fi

    echo "  Saving and importing..."
    docker save "${image}" | ${K3S_CTR} -

    echo "✓ ${image} loaded"
    echo ""
done

echo "============================================"
echo " Done!"
echo "============================================"
echo ""
echo "Verify with:  sudo k3s ctr images ls | grep ${IMAGE_PREFIX}"