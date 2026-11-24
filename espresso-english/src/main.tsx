import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import App from "./App";
import "./styles/base.css";
import "./styles/app.css";
import "./styles/lesson.css";
import "./styles/quiz.css";
import "./styles/pages.css";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <App />
  </StrictMode>
);

// Register the service worker so the app installs and works offline on iOS and Android.
if ("serviceWorker" in navigator) {
  window.addEventListener("load", () => {
    navigator.serviceWorker.register("./sw.js").catch(() => {
      /* offline support is a bonus — the app still runs without it */
    });
  });
}
