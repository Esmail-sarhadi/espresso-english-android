import type { Episode, Segment, VocabItem } from "./types";
import { hashString, shuffle } from "./format";

export type Question =
  | {
      kind: "meaning";
      term: string;
      ipa?: string;
      prompt: string;
      options: string[];
      answer: number;
      explain: string;
      fa: string;
    }
  | {
      kind: "blank";
      term: string;
      sentence: string;
      prompt: string;
      options: string[];
      answer: number;
      explain: string;
      fa: string;
      segmentId: string;
    }
  | {
      kind: "listen";
      segment: Segment;
      prompt: string;
      options: string[];
      answer: number;
      explain: string;
    }
  | {
      kind: "phrase";
      term: string;
      prompt: string;
      options: string[];
      answer: number;
      explain: string;
      fa: string;
    };

function pickDistractors<T>(pool: T[], correct: T, n: number, key: (v: T) => string): T[] {
  const seen = new Set([key(correct)]);
  const out: T[] = [];
  for (const item of pool) {
    if (out.length >= n) break;
    const k = key(item);
    if (seen.has(k)) continue;
    seen.add(k);
    out.push(item);
  }
  return out;
}

function buildOptions(correct: string, distractors: string[], seed: number) {
  const opts = shuffle([correct, ...distractors], seed);
  return { options: opts, answer: opts.indexOf(correct) };
}

export function buildQuiz(episode: Episode, maxQuestions = 10): Question[] {
  const seed = hashString(episode.id);
  const vocab = episode.vocab.filter((v) => v.fa && v.term);
  const questions: Question[] = [];

  /* ---- meaning questions ---- */
  const meaningPool = shuffle(vocab, seed).slice(0, 4);
  meaningPool.forEach((v, i) => {
    const distractors = pickDistractors(vocab, v, 3, (x) => x.fa).map((x) => x.fa);
    if (distractors.length < 3) return;
    const { options, answer } = buildOptions(v.fa, distractors, seed + i * 13);
    questions.push({
      kind: "meaning",
      term: v.term,
      ipa: v.ipa,
      prompt: "معنی این واژه کدام است؟",
      options,
      answer,
      explain: v.en ?? "",
      fa: v.fa,
    });
  });

  /* ---- fill in the blank ---- */
  const blankCandidates: { v: VocabItem; seg: Segment }[] = [];
  for (const v of vocab) {
    const term = v.term.toLowerCase();
    const seg = episode.segments.find((s) =>
      s.words.some((w) => w.w.replace(/[^A-Za-z']/g, "").toLowerCase() === term)
    );
    if (seg) blankCandidates.push({ v, seg });
  }
  shuffle(blankCandidates, seed + 7)
    .slice(0, 3)
    .forEach(({ v, seg }, i) => {
      const re = new RegExp(`\\b${escapeRegExp(v.term)}\\b`, "i");
      if (!re.test(seg.text)) return;
      const sentence = seg.text.replace(re, "______");
      const distractors = pickDistractors(vocab, v, 3, (x) => x.term.toLowerCase()).map(
        (x) => x.term
      );
      if (distractors.length < 3) return;
      const { options, answer } = buildOptions(v.term, distractors, seed + 31 + i);
      questions.push({
        kind: "blank",
        term: v.term,
        sentence,
        prompt: "جای خالی را کامل کنید",
        options,
        answer,
        explain: `${v.term} = ${v.fa}`,
        fa: v.fa,
        segmentId: seg.id,
      });
    });

  /* ---- listening ---- */
  const listenSegs = shuffle(
    episode.segments.filter((s) => s.words.length >= 5),
    seed + 17
  ).slice(0, 2);
  listenSegs.forEach((seg, i) => {
    const others = shuffle(
      episode.segments.filter((s) => s.id !== seg.id && s.text !== seg.text),
      seed + 53 + i
    )
      .slice(0, 3)
      .map((s) => s.text);
    if (others.length < 3) return;
    const { options, answer } = buildOptions(seg.text, others, seed + 71 + i);
    questions.push({
      kind: "listen",
      segment: seg,
      prompt: "کدام جمله را می‌شنوید؟",
      options,
      answer,
      explain: seg.fa ?? "",
    });
  });

  /* ---- phrases ---- */
  const phrases = episode.phrases ?? [];
  if (phrases.length) {
    shuffle(phrases, seed + 5)
      .slice(0, 2)
      .forEach((p, i) => {
        const distractors = pickDistractors(phrases, p, 3, (x) => x.fa).map((x) => x.fa);
        if (distractors.length < 3) return;
        const { options, answer } = buildOptions(p.fa, distractors, seed + 97 + i);
        questions.push({
          kind: "phrase",
          term: p.en,
          prompt: "معنی این عبارت چیست؟",
          options,
          answer,
          explain: p.note ?? "",
          fa: p.fa,
        });
      });
  }

  return shuffle(questions, seed + 3).slice(0, maxQuestions);
}

function escapeRegExp(s: string) {
  return s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
