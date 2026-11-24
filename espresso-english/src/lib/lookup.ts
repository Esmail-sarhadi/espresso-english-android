import type { Episode, Glossary, VocabItem } from "./types";
import { cleanWord } from "./format";

export interface LookupResult {
  term: string;
  fa?: string;
  ipa?: string;
  pos?: string;
  en?: string;
  example?: string;
  source: "lesson" | "glossary" | "online" | "none";
  loading?: boolean;
}

const CACHE_KEY = "espresso.studio.dictCache";

function readCache(): Record<string, LookupResult> {
  try {
    return JSON.parse(localStorage.getItem(CACHE_KEY) ?? "{}");
  } catch {
    return {};
  }
}

function writeCache(term: string, value: LookupResult) {
  try {
    const cache = readCache();
    cache[term] = value;
    const keys = Object.keys(cache);
    if (keys.length > 600) {
      for (const k of keys.slice(0, keys.length - 600)) delete cache[k];
    }
    localStorage.setItem(CACHE_KEY, JSON.stringify(cache));
  } catch {
    /* ignore */
  }
}

const IRREGULAR: Record<string, string> = {
  was: "be",
  were: "be",
  been: "be",
  am: "be",
  is: "be",
  are: "be",
  went: "go",
  gone: "go",
  said: "say",
  made: "make",
  took: "take",
  taken: "take",
  got: "get",
  gotten: "get",
  came: "come",
  saw: "see",
  seen: "see",
  knew: "know",
  known: "know",
  thought: "think",
  told: "tell",
  found: "find",
  gave: "give",
  given: "give",
  felt: "feel",
  left: "leave",
  kept: "keep",
  heard: "hear",
  held: "hold",
  brought: "bring",
  bought: "buy",
  began: "begin",
  begun: "begin",
  wrote: "write",
  written: "write",
  spoke: "speak",
  spoken: "speak",
  ran: "run",
  sat: "sit",
  stood: "stand",
  understood: "understand",
  lost: "lose",
  met: "meet",
  paid: "pay",
  sold: "sell",
  sent: "send",
  spent: "spend",
  taught: "teach",
  wore: "wear",
  won: "win",
  became: "become",
  better: "good",
  best: "good",
  worse: "bad",
  worst: "bad",
  children: "child",
  people: "person",
  men: "man",
  women: "woman",
  feet: "foot",
  teeth: "tooth",
};

export function variants(raw: string): string[] {
  const term = cleanWord(raw);
  const out = new Set<string>([term]);
  if (IRREGULAR[term]) out.add(IRREGULAR[term]);
  const rules: [RegExp, string][] = [
    [/ies$/, "y"],
    [/ves$/, "f"],
    [/ses$/, "s"],
    [/xes$/, "x"],
    [/ches$/, "ch"],
    [/shes$/, "sh"],
    [/es$/, ""],
    [/s$/, ""],
    [/ied$/, "y"],
    [/ed$/, ""],
    [/ed$/, "e"],
    [/ing$/, ""],
    [/ing$/, "e"],
    [/ly$/, ""],
    [/er$/, ""],
    [/est$/, ""],
  ];
  for (const [re, rep] of rules) {
    if (re.test(term)) out.add(term.replace(re, rep));
  }
  return [...out].filter((v) => v.length > 1);
}

export function findInLesson(episode: Episode | null, raw: string): VocabItem | undefined {
  if (!episode) return undefined;
  const cands = variants(raw);
  return episode.vocab.find((v) => cands.includes(v.term.toLowerCase()));
}

export function findInGlossary(glossary: Glossary, raw: string): { term: string; entry: Glossary[keyof Glossary] } | undefined {
  for (const v of variants(raw)) {
    if (glossary[v]) return { term: v, entry: glossary[v] };
  }
  return undefined;
}

async function fetchJson(url: string, ms = 6500) {
  const ctrl = new AbortController();
  const timer = setTimeout(() => ctrl.abort(), ms);
  try {
    const res = await fetch(url, { signal: ctrl.signal });
    if (!res.ok) return null;
    return await res.json();
  } catch {
    return null;
  } finally {
    clearTimeout(timer);
  }
}

export async function lookup(
  raw: string,
  episode: Episode | null,
  glossary: Glossary
): Promise<LookupResult> {
  const term = cleanWord(raw);
  if (!term) return { term: raw, source: "none" };

  const inLesson = findInLesson(episode, term);
  if (inLesson) {
    return {
      term: inLesson.term,
      fa: inLesson.fa,
      ipa: inLesson.ipa,
      pos: inLesson.pos,
      en: inLesson.en,
      example: inLesson.example,
      source: "lesson",
    };
  }

  const inGloss = findInGlossary(glossary, term);
  if (inGloss) {
    return {
      term: inGloss.term,
      fa: inGloss.entry.fa,
      ipa: inGloss.entry.ipa,
      pos: inGloss.entry.pos,
      en: inGloss.entry.en,
      example: inGloss.entry.example,
      source: "glossary",
    };
  }

  const cache = readCache();
  if (cache[term]) return cache[term];

  const [dict, trans] = await Promise.all([
    fetchJson(`https://api.dictionaryapi.dev/api/v2/entries/en/${encodeURIComponent(term)}`),
    fetchJson(
      `https://api.mymemory.translated.net/get?q=${encodeURIComponent(term)}&langpair=en|fa`
    ),
  ]);

  let result: LookupResult = { term, source: "none" };

  if (Array.isArray(dict) && dict.length) {
    const entry = dict[0];
    const phonetic =
      entry.phonetic ||
      entry.phonetics?.find((p: { text?: string }) => p.text)?.text ||
      "";
    const meaning = entry.meanings?.[0];
    result = {
      term,
      ipa: phonetic,
      pos: meaning?.partOfSpeech,
      en: meaning?.definitions?.[0]?.definition,
      example: meaning?.definitions?.[0]?.example,
      source: "online",
    };
  }

  const translated = trans?.responseData?.translatedText as string | undefined;
  if (translated && !/^NO QUERY|^MYMEMORY WARNING/i.test(translated)) {
    result.fa = translated;
  }

  if (result.fa || result.en) writeCache(term, result);
  return result;
}
