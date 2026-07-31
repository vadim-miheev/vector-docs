// Resolves frontend configuration at runtime.
//
// Priority:
//   1. `window.APP_CONFIG` — injected at runtime via /config.js (in Kubernetes
//      it is served by nginx from the vector-docs-config ConfigMap).
//   2. build-time `process.env.REACT_APP_*` — e.g. local development with a
//      `.env` file (Create React App inlines these during the build).
//   3. hardcoded default passed by the caller.

export function getRuntimeConfig() {
  if (typeof window !== 'undefined' && window.APP_CONFIG) {
    return window.APP_CONFIG;
  }
  return {};
}

/**
 * Resolve a configuration value.
 * @param {string} runtimeKey key inside window.APP_CONFIG
 * @param {string|undefined} buildEnvValue build-time REACT_APP_* value
 * @param {string} fallback hardcoded default
 */
export function resolveConfig(runtimeKey, buildEnvValue, fallback) {
  const cfg = getRuntimeConfig();
  if (cfg[runtimeKey] !== undefined && cfg[runtimeKey] !== null && cfg[runtimeKey] !== '') {
    return cfg[runtimeKey];
  }
  if (buildEnvValue !== undefined && buildEnvValue !== null && buildEnvValue !== '') {
    return buildEnvValue;
  }
  return fallback;
}