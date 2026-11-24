export type Level = "beginner" | "intermediate" | "advanced";
export type Category =
  | "grammar"
  | "vocabulary"
  | "phrases"
  | "pronunciation"
  | "idioms"
  | "conversation";

export interface Word {
  w: string;
  s: number;
  e: number;
}

export interface Segment {
  id: string;
  text: string;
  start: number;
  end: number;
  words: Word[];
  fa?: string;
}

export interface VocabItem {
  term: string;
  fa: string;
  pos?: string;
  ipa?: string;
  en?: string;
  example?: string;
}

export interface Phrase {
  en: string;
  fa: string;
  note?: string;
}

export interface Episode {
  id: string;
  title: string;
  titleFa: string;
  subtitle?: string;
  category: Category;
  level: Level;
  duration: number;
  audio: string;
  peaks: number[];
  descriptionFa: string;
  tags: string[];
  source: "espresso" | "local";
  published?: string;
  segments: Segment[];
  vocab: VocabItem[];
  phrases?: Phrase[];
  accent?: string;
  hero?: string;
}

export interface GlossaryEntry {
  fa: string;
  pos?: string;
  ipa?: string;
  en?: string;
  example?: string;
}

export type Glossary = Record<string, GlossaryEntry>;

export interface PodcastRef {
  name: string;
  host: string;
  level: Level;
  accent: string;
  url: string;
  descriptionFa: string;
  whyFa: string;
  tags: string[];
  badge?: string;
}

export interface SavedWord {
  term: string;
  fa: string;
  ipa?: string;
  pos?: string;
  en?: string;
  example?: string;
  episodeId?: string;
  episodeTitle?: string;
  segmentId?: string;
  createdAt: number;
  box: number;
  due: number;
  seen: number;
  correct: number;
}

export interface EpisodeProgress {
  position: number;
  duration: number;
  completed: boolean;
  plays: number;
  lastAt: number;
  listenSeconds: number;
}

export interface QuizResult {
  best: number;
  total: number;
  attempts: number;
  lastAt: number;
  history: { score: number; total: number; at: number }[];
}

export interface Settings {
  theme: "dark" | "light";
  translation: "off" | "on" | "blur";
  rate: number;
  autoScroll: boolean;
  fontScale: number;
  showIpa: boolean;
}
