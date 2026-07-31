// ============================================================================
// Runtime frontend configuration.
//
// This file ships as the default inside the built image and is OVERRIDDEN in
// Kubernetes by the `vector-docs-config` ConfigMap (k3s/configmap.yaml), which
// is mounted over /usr/share/nginx/html/config.js in the nginx pod.
//
// The app reads `window.APP_CONFIG` at runtime (see src/config/runtimeConfig.js).
// When a key is absent it falls back to the build-time REACT_APP_* env var, then
// to a hardcoded default. Keep this file environment-agnostic; set per-env values
// in k3s/configmap.yaml instead — no image rebuild is required.
//
// Keys recognized by the app:
//   API_BASE_URL  — base URL of the API gateway (default http://localhost:8080)
//   WS_URL        — WebSocket URL for notifications
//   DEMO_USER_ID  — user id that enables demo mode (0 disables it)
// ============================================================================
window.APP_CONFIG = {
  // API_BASE_URL: 'https://api.example.com',
  // WS_URL: 'wss://api.example.com/api/ws/notifications',
  // DEMO_USER_ID: '1',
};