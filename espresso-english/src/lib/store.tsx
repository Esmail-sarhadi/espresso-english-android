import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import type { Episode, QuizResult, SavedWord, Settings } from "./types";
import bundledEpisodes from "../data/episodes.json";
import glossaryData from "../data/glossary.json";
import podcastsData from "../data/podcasts.json";
import type { Glossary, PodcastRef } from "./types";
import {
  defaultSettings,
  getAudio,
  loadCustomEpisodes,
  readJSON,
  recordProgress,
  recordQuiz,
  saveCustomEpisodes,
  startPlay,
  writeJSON,
  type ProgressMap,
  type QuizMap,
} from "./storage";

/* -------------------------------- settings -------------------------------- */

interface SettingsCtx {
  settings: Settings;
  set: <K extends keyof Settings>(key: K, value: Settings[K]) => void;
}
const SettingsContext = createContext<SettingsCtx | null>(null);

export function SettingsProvider({ children }: { children: ReactNode }) {
  const [settings, setSettings] = useState<Settings>(() => ({
    ...defaultSettings,
    ...readJSON<Partial<Settings>>("settings", {}),
  }));

  useEffect(() => {
    writeJSON("settings", settings);
    document.documentElement.dataset.theme = settings.theme;
    document.documentElement.style.setProperty("--reading-scale", String(settings.fontScale));
  }, [settings]);

  const set = useCallback(<K extends keyof Settings>(key: K, value: Settings[K]) => {
    setSettings((prev) => ({ ...prev, [key]: value }));
  }, []);

  return (
    <SettingsContext.Provider value={{ settings, set }}>{children}</SettingsContext.Provider>
  );
}

export function useSettings() {
  const ctx = useContext(SettingsContext);
  if (!ctx) throw new Error("SettingsProvider missing");
  return ctx;
}

/* ---------------------------------- data ---------------------------------- */

interface DataCtx {
  episodes: Episode[];
  glossary: Glossary;
  podcasts: PodcastRef[];
  audioUrls: Record<string, string>;
  addCustomEpisode: (ep: Episode, blob: Blob) => Promise<void>;
  removeCustomEpisode: (id: string) => Promise<void>;
  getEpisode: (id: string) => Episode | undefined;
}
const DataContext = createContext<DataCtx | null>(null);

export function DataProvider({ children }: { children: ReactNode }) {
  const [custom, setCustom] = useState<Episode[]>(() => loadCustomEpisodes());
  const [audioUrls, setAudioUrls] = useState<Record<string, string>>({});

  useEffect(() => {
    let alive = true;
    (async () => {
      const urls: Record<string, string> = {};
      for (const ep of custom) {
        if (ep.source !== "local") continue;
        try {
          const blob = await getAudio(ep.id);
          if (blob) urls[ep.id] = URL.createObjectURL(blob);
        } catch {
          /* ignore */
        }
      }
      if (alive) setAudioUrls((prev) => ({ ...prev, ...urls }));
    })();
    return () => {
      alive = false;
    };
  }, [custom]);

  const episodes = useMemo(() => {
    const bundled = (bundledEpisodes as unknown as Episode[]).map((e) => ({ ...e }));
    return [...custom, ...bundled];
  }, [custom]);

  const addCustomEpisode = useCallback(async (ep: Episode, blob: Blob) => {
    const { putAudio } = await import("./storage");
    await putAudio(ep.id, blob);
    const next = [ep, ...loadCustomEpisodes()];
    saveCustomEpisodes(next);
    setCustom(next);
    setAudioUrls((prev) => ({ ...prev, [ep.id]: URL.createObjectURL(blob) }));
  }, []);

  const removeCustomEpisode = useCallback(async (id: string) => {
    const { deleteAudio } = await import("./storage");
    await deleteAudio(id);
    const next = loadCustomEpisodes().filter((e) => e.id !== id);
    saveCustomEpisodes(next);
    setCustom(next);
  }, []);

  const getEpisode = useCallback(
    (id: string) => episodes.find((e) => e.id === id),
    [episodes]
  );

  return (
    <DataContext.Provider
      value={{
        episodes,
        glossary: glossaryData as Glossary,
        podcasts: podcastsData as PodcastRef[],
        audioUrls,
        addCustomEpisode,
        removeCustomEpisode,
        getEpisode,
      }}
    >
      {children}
    </DataContext.Provider>
  );
}

export function useData() {
  const ctx = useContext(DataContext);
  if (!ctx) throw new Error("DataProvider missing");
  return ctx;
}

/* --------------------------------- player --------------------------------- */

interface PlayerCtx {
  episode: Episode | null;
  playing: boolean;
  time: number;
  duration: number;
  rate: number;
  ready: boolean;
  error: string | null;
  repeat: boolean;
  load: (episode: Episode, opts?: { seek?: number; autoplay?: boolean }) => void;
  toggle: () => void;
  play: () => void;
  pause: () => void;
  seek: (t: number) => void;
  skip: (delta: number) => void;
  setRate: (r: number) => void;
  setRepeat: (v: boolean) => void;
  playRange: (start: number, end: number) => void;
}
const PlayerContext = createContext<PlayerCtx | null>(null);

export function PlayerProvider({ children }: { children: ReactNode }) {
  const { settings } = useSettings();
  const { audioUrls, getEpisode } = useData();
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const rafRef = useRef<number>(0);
  const rangeRef = useRef<{ start: number; end: number } | null>(null);
  const lastTickRef = useRef<number>(0);
  const lastPersistRef = useRef<number>(0);

  const [episode, setEpisode] = useState<Episode | null>(null);
  const [playing, setPlaying] = useState(false);
  const [time, setTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [rate, setRateState] = useState(settings.rate);
  const [ready, setReady] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [repeat, setRepeat] = useState(false);

  const progressRef = useRef<ProgressMap>(readJSON<ProgressMap>("progress", {}));

  const persist = useCallback(
    (position: number, total: number, delta = 0) => {
      const ep = episode;
      if (!ep) return;
      progressRef.current = recordProgress(progressRef.current, ep.id, position, total, delta);
      writeJSON("progress", progressRef.current);
      window.dispatchEvent(new CustomEvent("espresso:progress"));
    },
    [episode]
  );

  const getAudioEl = useCallback(() => {
    if (!audioRef.current) {
      const el = new Audio();
      el.preload = "metadata";
      audioRef.current = el;
    }
    return audioRef.current;
  }, []);

  const load = useCallback(
    (ep: Episode, opts?: { seek?: number; autoplay?: boolean }) => {
      const el = getAudioEl();
      const src =
        ep.source === "local" ? audioUrls[ep.id] ?? "" : `${import.meta.env.BASE_URL}${ep.audio}`;
      if (!src) {
        setError("فایل صوتی این درس در دسترس نیست.");
        setEpisode(ep);
        return;
      }
      setError(null);
      setReady(false);
      if (episode?.id !== ep.id || el.src !== new URL(src, location.href).href) {
        el.src = src;
        el.load();
      }
      el.playbackRate = rate;
      setEpisode(ep);
      const startAt = opts?.seek ?? progressRef.current[ep.id]?.position ?? 0;
      setTime(startAt);
      const onMeta = () => {
        setDuration(el.duration || ep.duration || 0);
        setReady(true);
        if (startAt > 0 && startAt < (el.duration || Infinity) - 1) el.currentTime = startAt;
        el.removeEventListener("loadedmetadata", onMeta);
      };
      if (el.readyState >= 1) onMeta();
      else el.addEventListener("loadedmetadata", onMeta);

      progressRef.current = startPlay(progressRef.current, ep.id);
      writeJSON("progress", progressRef.current);
      window.dispatchEvent(new CustomEvent("espresso:progress"));

      if (opts?.autoplay) {
        el.play().then(() => setPlaying(true)).catch(() => setPlaying(false));
      } else {
        setPlaying(false);
      }
    },
    [audioUrls, episode?.id, getAudioEl, rate]
  );

  const play = useCallback(() => {
    const el = getAudioEl();
    el.play().then(() => setPlaying(true)).catch(() => setPlaying(false));
  }, [getAudioEl]);

  const pause = useCallback(() => {
    const el = getAudioEl();
    el.pause();
    setPlaying(false);
    persist(el.currentTime, el.duration || 0);
  }, [getAudioEl, persist]);

  const toggle = useCallback(() => {
    if (playing) pause();
    else play();
  }, [pause, play, playing]);

  const seek = useCallback((t: number) => {
    const el = getAudioEl();
    if (!Number.isFinite(t)) return;
    el.currentTime = Math.max(0, Math.min(t, el.duration || t));
    setTime(el.currentTime);
  }, [getAudioEl]);

  const skip = useCallback(
    (delta: number) => {
      const el = getAudioEl();
      seek(el.currentTime + delta);
    },
    [getAudioEl, seek]
  );

  const setRate = useCallback(
    (r: number) => {
      setRateState(r);
      getAudioEl().playbackRate = r;
    },
    [getAudioEl]
  );

  const playRange = useCallback(
    (start: number, end: number) => {
      rangeRef.current = { start, end };
      seek(start + 0.02);
      play();
    },
    [play, seek]
  );

  useEffect(() => {
    setRate(settings.rate);
  }, [settings.rate, setRate]);

  /* audio events */
  useEffect(() => {
    const el = getAudioEl();
    const onTime = () => {
      const t = el.currentTime;
      const now = performance.now();
      if (now - lastTickRef.current > 60) {
        lastTickRef.current = now;
        setTime(t);
      }
      const range = rangeRef.current;
      if (range && t >= range.end - 0.02) {
        if (repeat) {
          el.currentTime = range.start + 0.02;
        } else {
          rangeRef.current = null;
          el.pause();
          setPlaying(false);
        }
      }
      if (now - lastPersistRef.current > 5000 && !el.paused) {
        lastPersistRef.current = now;
        persist(t, el.duration || 0, 5);
      }
    };
    const onPlay = () => setPlaying(true);
    const onPause = () => setPlaying(false);
    const onEnd = () => {
      setPlaying(false);
      persist(el.duration, el.duration);
    };
    const onErr = () => setError("پخش فایل صوتی ممکن نشد.");
    el.addEventListener("timeupdate", onTime);
    el.addEventListener("play", onPlay);
    el.addEventListener("pause", onPause);
    el.addEventListener("ended", onEnd);
    el.addEventListener("error", onErr);
    return () => {
      el.removeEventListener("timeupdate", onTime);
      el.removeEventListener("play", onPlay);
      el.removeEventListener("pause", onPause);
      el.removeEventListener("ended", onEnd);
      el.removeEventListener("error", onErr);
    };
  }, [getAudioEl, persist, repeat]);

  /* smooth progress while playing */
  useEffect(() => {
    if (!playing) return;
    const tick = () => {
      const el = audioRef.current;
      if (el) setTime(el.currentTime);
      rafRef.current = requestAnimationFrame(tick);
    };
    rafRef.current = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(rafRef.current);
  }, [playing]);

  /* media session */
  useEffect(() => {
    if (!("mediaSession" in navigator) || !episode) return;
    navigator.mediaSession.metadata = new MediaMetadata({
      title: episode.title,
      artist: "Espresso English",
      album: episode.titleFa,
    });
    navigator.mediaSession.setActionHandler("play", () => play());
    navigator.mediaSession.setActionHandler("pause", () => pause());
    navigator.mediaSession.setActionHandler("seekbackward", () => skip(-5));
    navigator.mediaSession.setActionHandler("seekforward", () => skip(5));
  }, [episode, pause, play, skip]);

  const value = useMemo<PlayerCtx>(
    () => ({
      episode,
      playing,
      time,
      duration,
      rate,
      ready,
      error,
      repeat,
      load,
      toggle,
      play,
      pause,
      seek,
      skip,
      setRate,
      setRepeat,
      playRange,
    }),
    [
      episode,
      playing,
      time,
      duration,
      rate,
      ready,
      error,
      repeat,
      load,
      toggle,
      play,
      pause,
      seek,
      skip,
      setRate,
      playRange,
    ]
  );

  return <PlayerContext.Provider value={value}>{children}</PlayerContext.Provider>;
}

export function usePlayer() {
  const ctx = useContext(PlayerContext);
  if (!ctx) throw new Error("PlayerProvider missing");
  return ctx;
}

/* ------------------------------ user progress ----------------------------- */

export function useProgress() {
  const [map, setMap] = useState<ProgressMap>(() => readJSON<ProgressMap>("progress", {}));
  useEffect(() => {
    const update = () => setMap(readJSON<ProgressMap>("progress", {}));
    window.addEventListener("espresso:progress", update);
    window.addEventListener("storage", update);
    return () => {
      window.removeEventListener("espresso:progress", update);
      window.removeEventListener("storage", update);
    };
  }, []);
  return map;
}

export function useVocab() {
  const [list, setList] = useState<SavedWord[]>(() => readJSON<SavedWord[]>("vocab", []));
  const update = useCallback((next: SavedWord[]) => {
    setList(next);
    writeJSON("vocab", next);
    window.dispatchEvent(new CustomEvent("espresso:vocab"));
  }, []);
  useEffect(() => {
    const sync = () => setList(readJSON<SavedWord[]>("vocab", []));
    window.addEventListener("espresso:vocab", sync);
    window.addEventListener("storage", sync);
    return () => {
      window.removeEventListener("espresso:vocab", sync);
      window.removeEventListener("storage", sync);
    };
  }, []);
  return { vocab: list, setVocab: update };
}

export function useQuizResults() {
  const [map, setMap] = useState<QuizMap>(() => readJSON<QuizMap>("quiz", {}));
  const save = useCallback((episodeId: string, score: number, total: number) => {
    const next = recordQuiz(readJSON<QuizMap>("quiz", {}), episodeId, score, total);
    writeJSON("quiz", next);
    setMap(next);
    window.dispatchEvent(new CustomEvent("espresso:quiz"));
  }, []);
  useEffect(() => {
    const sync = () => setMap(readJSON<QuizMap>("quiz", {}));
    window.addEventListener("espresso:quiz", sync);
    return () => window.removeEventListener("espresso:quiz", sync);
  }, []);
  return { results: map as Record<string, QuizResult>, save };
}
