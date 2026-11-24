import { useEffect, useState } from "react";
import { Sidebar, Topbar } from "./components/Layout";
import { Home } from "./pages/Home";
import { Library } from "./pages/Library";
import { Lesson } from "./pages/Lesson";
import { Notebook } from "./pages/Notebook";
import { Explore } from "./pages/Explore";
import { ImportLesson } from "./pages/ImportLesson";
import { Review } from "./pages/Review";
import { DataProvider, PlayerProvider, SettingsProvider } from "./lib/store";

export interface Route {
  name: string;
  id?: string;
  query: string;
}

export function parseRoute(): Route {
  const raw = window.location.hash.replace(/^#\/?/, "");
  const [path, query = ""] = raw.split("?");
  const [name, id] = path.split("/");
  return { name: name || "home", id, query };
}

export function navigate(to: string) {
  if (window.location.hash === `#/${to}`) return;
  window.location.hash = `#/${to}`;
  window.scrollTo({ top: 0, behavior: "instant" as ScrollBehavior });
}

function useRoute(): Route {
  const [route, setRoute] = useState<Route>(() => parseRoute());
  useEffect(() => {
    const onChange = () => {
      setRoute(parseRoute());
      window.scrollTo({ top: 0, behavior: "instant" as ScrollBehavior });
    };
    window.addEventListener("hashchange", onChange);
    return () => window.removeEventListener("hashchange", onChange);
  }, []);
  return route;
}

function Router() {
  const route = useRoute();

  let page: React.ReactNode = null;
  switch (route.name) {
    case "library":
      page = <Library query={route.query} />;
      break;
    case "lesson":
      page = <Lesson id={route.id ?? ""} />;
      break;
    case "notebook":
      page = <Notebook />;
      break;
    case "explore":
      page = <Explore />;
      break;
    case "import":
      page = <ImportLesson />;
      break;
    case "review":
      page = <Review />;
      break;
    default:
      page = <Home />;
  }

  return (
    <div className="shell">
      <Sidebar route={route} />
      <div className="main">
        <Topbar route={route} />
        <main className="page" key={route.name + (route.id ?? "")}>
          {page}
        </main>
      </div>
    </div>
  );
}

export default function App() {
  return (
    <SettingsProvider>
      <DataProvider>
        <PlayerProvider>
          <Router />
        </PlayerProvider>
      </DataProvider>
    </SettingsProvider>
  );
}
