import type { CapacitorConfig } from "@capacitor/cli";

/**
 * Native iOS/Android shell around the existing React app.
 *
 * The web build in `dist/` is copied into the native project by `npx cap sync`, so the same code
 * powers the browser, the PWA and the App Store build — one source of truth.
 */
const config: CapacitorConfig = {
  appId: "ir.esmail.espresso",
  appName: "Espresso English",
  webDir: "dist",
  backgroundColor: "#0C0A10",
  ios: {
    // Let the lesson text run under the notch/status bar like it does on Android.
    contentInset: "always",
    backgroundColor: "#0C0A10",
    // Hash routing keeps every route inside the bundle, so no server is needed.
    limitsNavigationsToAppBoundDomains: false,
    preferredContentMode: "mobile",
  },
  android: {
    backgroundColor: "#0C0A10",
    allowMixedContent: false,
  },
  server: {
    androidScheme: "https",
    iosScheme: "capacitor",
  },
};

export default config;
