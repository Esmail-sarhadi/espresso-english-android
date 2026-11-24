const FA_DIGITS = ["۰", "۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹"];

export function fa(value: number | string): string {
  return String(value).replace(/[0-9]/g, (d) => FA_DIGITS[Number(d)]);
}

export function faNum(value: number): string {
  return fa(Math.round(value).toLocaleString("en-US"));
}

export function fmtTime(sec: number): string {
  if (!Number.isFinite(sec) || sec < 0) sec = 0;
  const m = Math.floor(sec / 60);
  const s = Math.floor(sec % 60);
  return `${m}:${s.toString().padStart(2, "0")}`;
}

export function fmtTimeFa(sec: number): string {
  return fa(fmtTime(sec));
}

export function fmtMinutes(sec: number): string {
  const m = Math.round(sec / 60);
  return `${fa(Math.max(1, m))} دقیقه`;
}

export function cleanWord(raw: string): string {
  return raw
    .replace(/^[^A-Za-z'’-]+|[^A-Za-z'’-]+$/g, "")
    .replace(/[’]/g, "'")
    .toLowerCase();
}

export function displayWord(raw: string): string {
  return raw.replace(/^[^A-Za-z'’-]+|[^A-Za-z'’-]+$/g, "");
}

export const categoryLabels: Record<string, string> = {
  grammar: "گرامر",
  vocabulary: "واژگان",
  phrases: "عبارت‌ها",
  pronunciation: "تلفظ",
  idioms: "اصطلاح‌ها",
  conversation: "مکالمه",
};

export const categoryColors: Record<string, string> = {
  grammar: "var(--c-grammar)",
  vocabulary: "var(--c-vocab)",
  phrases: "var(--c-phrases)",
  pronunciation: "var(--c-pron)",
  idioms: "var(--c-idioms)",
  conversation: "var(--c-conv)",
};

export const levelLabels: Record<string, string> = {
  beginner: "مبتدی",
  intermediate: "متوسط",
  advanced: "پیشرفته",
};

export const levelColors: Record<string, string> = {
  beginner: "var(--good)",
  intermediate: "var(--accent)",
  advanced: "var(--bad)",
};

export function categoryGradient(category: string): string {
  const map: Record<string, string> = {
    grammar: "linear-gradient(135deg,#1d3346,#2f5b7a)",
    vocabulary: "linear-gradient(135deg,#3a2716,#8a5a2b)",
    phrases: "linear-gradient(135deg,#33203d,#6c4a7d)",
    pronunciation: "linear-gradient(135deg,#173328,#3c7259)",
    idioms: "linear-gradient(135deg,#3d211c,#7d4436)",
    conversation: "linear-gradient(135deg,#332c17,#7a6630)",
  };
  return map[category] ?? "linear-gradient(135deg,#2a211c,#4a382c)";
}

export function todayKey(d = new Date()): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(
    d.getDate()
  ).padStart(2, "0")}`;
}

export function relativeFa(ts: number): string {
  const diff = Date.now() - ts;
  const min = Math.round(diff / 60000);
  if (min < 1) return "همین حالا";
  if (min < 60) return `${fa(min)} دقیقه پیش`;
  const h = Math.round(min / 60);
  if (h < 24) return `${fa(h)} ساعت پیش`;
  const d = Math.round(h / 24);
  if (d < 30) return `${fa(d)} روز پیش`;
  return `${fa(Math.round(d / 30))} ماه پیش`;
}

export function clamp(v: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, v));
}

export function shuffle<T>(arr: T[], seed = 1): T[] {
  const out = [...arr];
  let s = seed;
  const rand = () => {
    s = (s * 9301 + 49297) % 233280;
    return s / 233280;
  };
  for (let i = out.length - 1; i > 0; i--) {
    const j = Math.floor(rand() * (i + 1));
    [out[i], out[j]] = [out[j], out[i]];
  }
  return out;
}

export function hashString(str: string): number {
  let h = 2166136261;
  for (let i = 0; i < str.length; i++) {
    h ^= str.charCodeAt(i);
    h = Math.imul(h, 16777619);
  }
  return Math.abs(h);
}

/** Split a plain-text transcript into sentence-ish segments. */
export function splitSentences(text: string): string[] {
  const normalized = text.replace(/\r/g, "").replace(/\n{2,}/g, "\n").trim();
  const out: string[] = [];
  for (const block of normalized.split("\n")) {
    const line = block.trim();
    if (!line) continue;
    const parts = line.match(/[^.!?…]+[.!?…]+["')\]]*|[^.!?…]+$/g) ?? [line];
    for (const p of parts) {
      const t = p.trim();
      if (t) out.push(t);
    }
  }
  return out;
}

export interface Cue {
  start: number;
  end: number;
  text: string;
}

export function parseTimestamp(raw: string): number {
  const m = raw.trim().match(/(\d+):(\d{2}):(\d{2})[.,](\d{1,3})|(\d+):(\d{2})[.,](\d{1,3})/);
  if (!m) return 0;
  if (m[1] !== undefined) {
    return Number(m[1]) * 3600 + Number(m[2]) * 60 + Number(m[3]) + Number(m[4].padEnd(3, "0")) / 1000;
  }
  return Number(m[5]) * 60 + Number(m[6]) + Number(m[7].padEnd(3, "0")) / 1000;
}

export function parseSubtitles(raw: string): Cue[] {
  const text = raw.replace(/\r/g, "").replace(/^\uFEFF/, "");
  if (/^\s*WEBVTT/i.test(text)) {
    return text
      .split(/\n{2,}/)
      .map((block) => {
        const lines = block.split("\n").filter((l) => l.trim() && !/^\s*WEBVTT/i.test(l));
        const timeLine = lines.find((l) => l.includes("-->"));
        if (!timeLine) return null;
        const [a, b] = timeLine.split("-->");
        const content = lines
          .filter((l) => l !== timeLine && !/^\d+$/.test(l.trim()))
          .join(" ")
          .trim();
        if (!content) return null;
        return { start: parseTimestamp(a), end: parseTimestamp(b), text: content };
      })
      .filter(Boolean) as Cue[];
  }
  return text
    .split(/\n{2,}/)
    .map((block) => {
      const lines = block.split("\n");
      const timeLine = lines.find((l) => l.includes("-->"));
      if (!timeLine) return null;
      const [a, b] = timeLine.split("-->");
      const content = lines
        .filter((l) => l !== timeLine && !/^\d+$/.test(l.trim()))
        .join(" ")
        .trim();
      if (!content) return null;
      return { start: parseTimestamp(a), end: parseTimestamp(b), text: content };
    })
    .filter(Boolean) as Cue[];
}

export function wordsFromText(text: string, start: number, end: number) {
  const tokens = text.split(/\s+/).filter(Boolean);
  const weights = tokens.map((t) => Math.max(1, t.replace(/[^A-Za-z0-9']/g, "").length));
  const total = weights.reduce((a, b) => a + b, 0) || 1;
  const span = Math.max(0.15, end - start);
  let cursor = start;
  return tokens.map((t, i) => {
    const w = (weights[i] ?? 1) / total;
    const dur = span * w;
    const word = { w: t, s: round2(cursor), e: round2(cursor + dur) };
    cursor += dur;
    return word;
  });
}

function round2(n: number) {
  return Math.round(n * 100) / 100;
}
