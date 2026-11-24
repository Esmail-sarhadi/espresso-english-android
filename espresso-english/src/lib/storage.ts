import type {
  Episode,
  EpisodeProgress,
  QuizResult,
  SavedWord,
  Settings,
} from "./types";

const NS = "espresso.studio.";

export function readJSON<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(NS + key);
    if (!raw) return fallback;
    return JSON.parse(raw) as T;
  } catch {
    return fallback;
  }
}

export function writeJSON<T>(key: string, value: T) {
  try {
    localStorage.setItem(NS + key, JSON.stringify(value));
  } catch {
    /* quota */
  }
}

export const defaultSettings: Settings = {
  theme: "dark",
  translation: "on",
  rate: 1,
  autoScroll: true,
  fontScale: 1,
  showIpa: true,
};

/* ------------------------------ progress ------------------------------ */

export type ProgressMap = Record<string, EpisodeProgress>;

export function recordProgress(
  map: ProgressMap,
  episodeId: string,
  position: number,
  duration: number,
  deltaSeconds = 0
): ProgressMap {
  const prev = map[episodeId];
  const next: EpisodeProgress = {
    position,
    duration: duration || prev?.duration || 0,
    completed: Boolean(prev?.completed) || (duration > 0 && position / duration > 0.95),
    plays: prev?.plays ?? 0,
    lastAt: Date.now(),
    listenSeconds: (prev?.listenSeconds ?? 0) + Math.max(0, deltaSeconds),
  };
  return { ...map, [episodeId]: next };
}

export function startPlay(map: ProgressMap, episodeId: string): ProgressMap {
  const prev = map[episodeId];
  return {
    ...map,
    [episodeId]: {
      position: prev?.position ?? 0,
      duration: prev?.duration ?? 0,
      completed: prev?.completed ?? false,
      plays: (prev?.plays ?? 0) + 1,
      lastAt: Date.now(),
      listenSeconds: prev?.listenSeconds ?? 0,
    },
  };
}

/* ------------------------------ vocabulary ---------------------------- */

const BOX_DAYS = [0, 1, 3, 7, 16, 35];

export function addWord(list: SavedWord[], word: Omit<SavedWord, "createdAt" | "box" | "due" | "seen" | "correct">): SavedWord[] {
  const key = word.term.toLowerCase();
  if (list.some((w) => w.term.toLowerCase() === key)) return list;
  const now = Date.now();
  return [
    {
      ...word,
      createdAt: now,
      box: 0,
      due: now,
      seen: 0,
      correct: 0,
    },
    ...list,
  ];
}

export function reviewWord(list: SavedWord[], term: string, knew: boolean): SavedWord[] {
  return list.map((w) => {
    if (w.term.toLowerCase() !== term.toLowerCase()) return w;
    const box = knew ? Math.min(5, w.box + 1) : 0;
    const days = BOX_DAYS[box] ?? 1;
    return {
      ...w,
      box,
      seen: w.seen + 1,
      correct: w.correct + (knew ? 1 : 0),
      due: Date.now() + days * 86400000,
    };
  });
}

export function dueWords(list: SavedWord[]): SavedWord[] {
  const now = Date.now();
  return list.filter((w) => w.due <= now);
}

/* ------------------------------ quiz results -------------------------- */

export type QuizMap = Record<string, QuizResult>;

export function recordQuiz(map: QuizMap, episodeId: string, score: number, total: number): QuizMap {
  const prev = map[episodeId];
  const pct = total ? Math.round((score / total) * 100) : 0;
  const best = Math.max(prev?.best ?? 0, pct);
  return {
    ...map,
    [episodeId]: {
      best,
      total,
      attempts: (prev?.attempts ?? 0) + 1,
      lastAt: Date.now(),
      history: [...(prev?.history ?? []), { score, total, at: Date.now() }].slice(-20),
    },
  };
}

/* ------------------------------ local audio --------------------------- */

const DB_NAME = "espresso-studio";
const STORE = "audio";

function openDB(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, 1);
    req.onupgradeneeded = () => {
      const db = req.result;
      if (!db.objectStoreNames.contains(STORE)) db.createObjectStore(STORE);
    };
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
}

export async function putAudio(id: string, blob: Blob): Promise<void> {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE, "readwrite");
    tx.objectStore(STORE).put(blob, id);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

export async function getAudio(id: string): Promise<Blob | undefined> {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE, "readonly");
    const req = tx.objectStore(STORE).get(id);
    req.onsuccess = () => resolve(req.result as Blob | undefined);
    req.onerror = () => reject(req.error);
  });
}

export async function deleteAudio(id: string): Promise<void> {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE, "readwrite");
    tx.objectStore(STORE).delete(id);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

/* ------------------------------ custom episodes ----------------------- */

export type StoredEpisode = Episode & { local?: boolean };

export function loadCustomEpisodes(): Episode[] {
  return readJSON<Episode[]>("customEpisodes", []);
}

export function saveCustomEpisodes(list: Episode[]) {
  writeJSON("customEpisodes", list);
}
