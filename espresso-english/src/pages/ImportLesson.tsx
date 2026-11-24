import { useEffect, useMemo, useRef, useState } from "react";
import { navigate } from "../App";
import { Icon } from "../components/Icon";
import { useData } from "../lib/store";
import {
  fmtTime,
  parseSubtitles,
  splitSentences,
  wordsFromText,
  type Cue,
} from "../lib/format";
import type { Category, Episode, Level, Segment } from "../lib/types";

const STOP = new Set(
  "the a an and or but if so of to in on at for with from by as is are was were be been being do does did have has had i you he she it we they me him her us them my your his its our their this that these those there here not no yes can could will would should may might must about into over after before than then just also very really more most much many some any all one two three what when where who whom which how why because while during through up down out off again".split(
    /\s+/
  )
);

type Step = 1 | 2 | 3 | 4;

export function ImportLesson() {
  const { addCustomEpisode, episodes, removeCustomEpisode } = useData();
  const [step, setStep] = useState<Step>(1);
  const [file, setFile] = useState<File | null>(null);
  const [audioUrl, setAudioUrl] = useState<string>("");
  const [duration, setDuration] = useState(0);
  const [peaks, setPeaks] = useState<number[]>([]);
  const [rawText, setRawText] = useState("");
  const [cues, setCues] = useState<Cue[] | null>(null);
  const [segments, setSegments] = useState<Segment[]>([]);
  const [title, setTitle] = useState("");
  const [titleFa, setTitleFa] = useState("");
  const [level, setLevel] = useState<Level>("intermediate");
  const [category, setCategory] = useState<Category>("vocabulary");
  const [desc, setDesc] = useState("");
  const [saving, setSaving] = useState(false);
  const [saveNote, setSaveNote] = useState("");
  const [over, setOver] = useState(false);
  const audioRef = useRef<HTMLAudioElement>(null);
  const [now, setNow] = useState(0);
  const [playing, setPlaying] = useState(false);

  const custom = episodes.filter((e) => e.source === "local");

  useEffect(() => {
    return () => {
      if (audioUrl) URL.revokeObjectURL(audioUrl);
    };
  }, [audioUrl]);

  useEffect(() => {
    const el = audioRef.current;
    if (!el) return;
    const onTime = () => setNow(el.currentTime);
    el.addEventListener("timeupdate", onTime);
    el.addEventListener("play", () => setPlaying(true));
    el.addEventListener("pause", () => setPlaying(false));
    return () => el.removeEventListener("timeupdate", onTime);
  }, [audioUrl, step]);

  const onFile = async (f: File) => {
    setFile(f);
    const url = URL.createObjectURL(f);
    setAudioUrl(url);
    const el = new Audio(url);
    el.addEventListener("loadedmetadata", () => {
      setDuration(el.duration || 0);
    });
    try {
      const ctx = new (window.AudioContext || (window as any).webkitAudioContext)();
      const buf = await ctx.decodeAudioData(await f.arrayBuffer());
      const data = buf.getChannelData(0);
      const n = 110;
      const stepSize = Math.floor(data.length / n) || 1;
      const arr: number[] = [];
      for (let i = 0; i < n; i++) {
        let max = 0;
        for (let j = i * stepSize; j < (i + 1) * stepSize; j += 24) {
          const v = Math.abs(data[j] || 0);
          if (v > max) max = v;
        }
        arr.push(max);
      }
      const m = Math.max(...arr) || 1;
      setPeaks(arr.map((v) => Math.max(8, Math.round((v / m) * 100))));
      await ctx.close();
    } catch {
      setPeaks(Array.from({ length: 110 }, () => 40));
    }
  };

  const parseText = (text: string) => {
    if (/-->/.test(text)) {
      const parsed = parseSubtitles(text);
      setCues(parsed);
      setSegments(
        parsed.map((c, i) => ({
          id: `seg-${i}`,
          text: c.text,
          start: c.start,
          end: c.end,
          words: wordsFromText(c.text, c.start, c.end),
        }))
      );
      return;
    }
    setCues(null);
    const lines = splitSentences(text);
    const est = duration || lines.length * 4;
    const totalWords = lines.reduce((s, l) => s + l.split(/\s+/).length, 0) || 1;
    let cursor = 0;
    setSegments(
      lines.map((l, i) => {
        const share = (l.split(/\s+/).length / totalWords) * est;
        const start = cursor;
        cursor += share;
        return {
          id: `seg-${i}`,
          text: l,
          start,
          end: cursor,
          words: wordsFromText(l, start, cursor),
        };
      })
    );
  };

  const setStart = (idx: number) => {
    const t = audioRef.current?.currentTime ?? now;
    setSegments((prev) => {
      const next = [...prev];
      next[idx] = { ...next[idx], start: t, words: wordsFromText(next[idx].text, t, next[idx].end) };
      return next;
    });
  };

  const autoDistribute = () => {
    const est = duration || segments.length * 4;
    const totalWords = segments.reduce((s, x) => s + x.text.split(/\s+/).length, 0) || 1;
    let cursor = 0;
    setSegments((prev) =>
      prev.map((s) => {
        const share = (s.text.split(/\s+/).length / totalWords) * est;
        const start = cursor;
        cursor += share;
        return { ...s, start, end: cursor, words: wordsFromText(s.text, start, cursor) };
      })
    );
  };

  const finalizeSegments = (): Segment[] => {
    const sorted = [...segments].sort((a, b) => a.start - b.start);
    return sorted.map((s, i) => {
      const next = sorted[i + 1];
      const end = next ? Math.max(s.start + 0.6, Math.min(next.start, s.start + 12)) : duration || s.end;
      return {
        ...s,
        id: `s${i + 1}`,
        end,
        words: wordsFromText(s.text, s.start, end),
      };
    });
  };

  const save = async () => {
    if (!file) return;
    setSaving(true);
    setSaveNote("در حال ساخت درس…");
    const id = `local-${Date.now()}`;
    const finalSegs = finalizeSegments();

    // auto vocabulary
    const freq = new Map<string, number>();
    for (const s of finalSegs) {
      for (const w of s.text.toLowerCase().split(/[^a-z'-]+/)) {
        if (!w || w.length < 4 || STOP.has(w)) continue;
        freq.set(w, (freq.get(w) ?? 0) + 1);
      }
    }
    const terms = [...freq.entries()]
      .sort((a, b) => b[1] - a[1])
      .slice(0, 10)
      .map(([w]) => w);

    const vocab = [];
    for (let i = 0; i < terms.length; i++) {
      const term = terms[i];
      setSaveNote(`در حال ترجمه واژه‌ها… (${i + 1}/${terms.length})`);
      let fa = "";
      try {
        const ctrl = new AbortController();
        const timer = setTimeout(() => ctrl.abort(), 5000);
        const res = await fetch(
          `https://api.mymemory.translated.net/get?q=${encodeURIComponent(term)}&langpair=en|fa`,
          { signal: ctrl.signal }
        );
        clearTimeout(timer);
        const json = await res.json();
        const t = json?.responseData?.translatedText as string | undefined;
        if (t && !/^NO QUERY|MYMEMORY WARNING/i.test(t)) fa = t;
      } catch {
        /* skip */
      }
      vocab.push({ term, fa: fa || "—", pos: "" });
    }

    const ep: Episode = {
      id,
      title: title || file.name.replace(/\.[^.]+$/, ""),
      titleFa: titleFa || title || "درس من",
      subtitle: "افزوده‌شده توسط من",
      category,
      level,
      duration: duration || finalSegs.at(-1)?.end || 0,
      audio: "",
      peaks: peaks.length ? peaks : Array.from({ length: 110 }, () => 40),
      descriptionFa: desc || "درسی که خودم از فایل صوتی و متن ساختم.",
      tags: ["درس من"],
      source: "local",
      segments: finalSegs,
      vocab,
      phrases: [],
    };

    await addCustomEpisode(ep, file);
    setSaving(false);
    navigate(`lesson/${id}`);
  };

  const steps = [
    { n: 1, label: "فایل صوتی" },
    { n: 2, label: "متن" },
    { n: 3, label: "همگام‌سازی" },
    { n: 4, label: "اطلاعات درس" },
  ];

  return (
    <div className="import-wrap">
      <div className="page-head">
        <div>
          <h1 className="page-title">افزودن درس از فایل خودت</h1>
          <p className="page-sub">
            mp3 + متن (یا فایل srt/vtt) بده تا همین زیرنویس واژه‌به‌واژه و آزمون برایش ساخته شود.
          </p>
        </div>
      </div>

      <div className="steps-bar">
        {steps.map((s) => (
          <div
            key={s.n}
            className="step-pill"
            data-active={step === s.n}
            data-done={step > s.n}
          >
            <b>{step > s.n ? "✓" : s.n}</b>
            {s.label}
          </div>
        ))}
      </div>

      {step === 1 && (
        <div className="card import-card">
          <h2>۱. فایل صوتی</h2>
          <p>فایل mp3، m4a، wav یا ogg. فایل روی همین دستگاه می‌ماند و جایی آپلود نمی‌شود.</p>
          <label
            className={`dropzone${over ? " is-over" : ""}`}
            onDragOver={(e) => {
              e.preventDefault();
              setOver(true);
            }}
            onDragLeave={() => setOver(false)}
            onDrop={(e) => {
              e.preventDefault();
              setOver(false);
              const f = e.dataTransfer.files[0];
              if (f) onFile(f);
            }}
          >
            <input
              type="file"
              accept="audio/*,.mp3,.m4a,.wav,.ogg"
              hidden
              onChange={(e) => {
                const f = e.target.files?.[0];
                if (f) onFile(f);
              }}
            />
            <Icon name="upload" />
            <b>{file ? "فایل انتخاب شد" : "فایل را اینجا رها کن یا کلیک کن"}</b>
            <span>{file ? "برای تغییر، فایل دیگری انتخاب کن" : "حداکثر چند دقیقه صدا کافی است"}</span>
          </label>

          {file && (
            <div className="file-chip">
              <Icon name="headphones" width={16} height={16} />
              <span className="grow">{file.name}</span>
              <span className="num muted">
                {fmtTime(duration)} · {(file.size / 1024 / 1024).toFixed(1)}MB
              </span>
            </div>
          )}

          <div className="import-actions">
            <span />
            <button className="btn btn-primary" disabled={!file} onClick={() => setStep(2)}>
              مرحله بعد
              <Icon name="arrowLeft" width={14} height={14} />
            </button>
          </div>
        </div>
      )}

      {step === 2 && (
        <div className="card import-card">
          <h2>۲. متن درس</h2>
          <p>
            متن را بچسبان یا فایل زیرنویس (srt / vtt) را انتخاب کن. اگر فایل زیرنویس داشته
            باشی، زمان‌بندی خودکار انجام می‌شود.
          </p>
          <label className="btn btn-outline btn-sm" style={{ marginBottom: 12 }}>
            <Icon name="upload" width={14} height={14} />
            انتخاب فایل srt / vtt / txt
            <input
              type="file"
              accept=".srt,.vtt,.txt"
              hidden
              onChange={async (e) => {
                const f = e.target.files?.[0];
                if (!f) return;
                const text = await f.text();
                setRawText(text);
                parseText(text);
              }}
            />
          </label>
          <textarea
            className="input"
            rows={10}
            placeholder={"Hi students! Today we're going to learn…\n\nمتن انگلیسی را اینجا بگذار…"}
            value={rawText}
            onChange={(e) => {
              setRawText(e.target.value);
              parseText(e.target.value);
            }}
            style={{ direction: "ltr", textAlign: "left", fontFamily: "var(--font-ui)" }}
          />
          <div className="muted" style={{ fontSize: 12, marginTop: 10 }}>
            {segments.length
              ? `${segments.length} جمله شناسایی شد${cues ? " (با زمان‌بندی از فایل زیرنویس)" : ""}.`
              : "هنوز متنی وارد نشده است."}
          </div>

          <div className="import-actions">
            <button className="btn btn-ghost" onClick={() => setStep(1)}>
              <Icon name="chevron" width={14} height={14} style={{ transform: "rotate(180deg)" }} />
              قبلی
            </button>
            <button
              className="btn btn-primary"
              disabled={!segments.length}
              onClick={() => setStep(3)}
            >
              مرحله بعد
              <Icon name="arrowLeft" width={14} height={14} />
            </button>
          </div>
        </div>
      )}

      {step === 3 && (
        <div className="card import-card">
          <h2>۳. همگام‌سازی</h2>
          <p>
            {cues
              ? "زمان‌بندی از فایل زیرنویس خوانده شد. اگر لازم بود، زمان شروع هر جمله را دستی اصلاح کن."
              : "زمان‌ها تخمینی تقسیم شده‌اند. صدا را پخش کن و برای هر جمله، وقتی گوینده شروع کرد دکمه‌ی «ثبت» را بزن."}
          </p>

          {audioUrl && (
            <audio
              ref={audioRef}
              src={audioUrl}
              controls
              style={{ width: "100%", marginBottom: 12 }}
              onTimeUpdate={(e) => setNow(e.currentTarget.currentTime)}
            />
          )}

          <div style={{ display: "flex", gap: 8, marginBottom: 8 }}>
            <button className="btn btn-sm btn-outline" onClick={autoDistribute}>
              <Icon name="sliders" width={13} height={13} />
              تقسیم خودکار زمان
            </button>
            <button
              className="btn btn-sm btn-ghost"
              onClick={() => {
                const el = audioRef.current;
                if (!el) return;
                if (el.paused) el.play();
                else el.pause();
              }}
            >
              <Icon name={playing ? "pause" : "play"} width={13} height={13} />
              {playing ? "توقف" : "پخش"}
            </button>
            <span className="num muted" style={{ marginInlineStart: "auto", fontSize: 12 }}>
              {fmtTime(now)} / {fmtTime(duration)}
            </span>
          </div>

          <div className="timing-list">
            {segments.map((s, i) => (
              <div key={i} className="timing-row">
                <span className="idx num">{i + 1}</span>
                <span className="txt">{s.text}</span>
                <span className="t num">{fmtTime(s.start)}</span>
                <button className="set" onClick={() => setStart(i)}>
                  ثبت زمان
                </button>
              </div>
            ))}
          </div>

          <div className="import-actions">
            <button className="btn btn-ghost" onClick={() => setStep(2)}>
              <Icon name="chevron" width={14} height={14} style={{ transform: "rotate(180deg)" }} />
              قبلی
            </button>
            <button className="btn btn-primary" onClick={() => setStep(4)}>
              مرحله بعد
              <Icon name="arrowLeft" width={14} height={14} />
            </button>
          </div>
        </div>
      )}

      {step === 4 && (
        <div className="card import-card">
          <h2>۴. اطلاعات درس</h2>
          <p>این اطلاعات در کتابخانه و کارت درس نمایش داده می‌شود.</p>
          <div className="form-grid">
            <div className="field">
              <label>عنوان انگلیسی</label>
              <input
                className="input en"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="My First Lesson"
              />
            </div>
            <div className="field">
              <label>عنوان فارسی</label>
              <input
                className="input"
                value={titleFa}
                onChange={(e) => setTitleFa(e.target.value)}
                placeholder="درس اول من"
              />
            </div>
            <div className="field">
              <label>سطح</label>
              <select className="input" value={level} onChange={(e) => setLevel(e.target.value as Level)}>
                <option value="beginner">مبتدی</option>
                <option value="intermediate">متوسط</option>
                <option value="advanced">پیشرفته</option>
              </select>
            </div>
            <div className="field">
              <label>دسته</label>
              <select
                className="input"
                value={category}
                onChange={(e) => setCategory(e.target.value as Category)}
              >
                <option value="vocabulary">واژگان</option>
                <option value="grammar">گرامر</option>
                <option value="phrases">عبارت‌ها</option>
                <option value="pronunciation">تلفظ</option>
                <option value="idioms">اصطلاح‌ها</option>
                <option value="conversation">مکالمه</option>
              </select>
            </div>
            <div className="field full">
              <label>توضیح کوتاه</label>
              <textarea
                className="input"
                rows={3}
                value={desc}
                onChange={(e) => setDesc(e.target.value)}
                placeholder="این درس درباره…"
              />
            </div>
          </div>

          <div className="import-actions">
            <button className="btn btn-ghost" onClick={() => setStep(3)} disabled={saving}>
              <Icon name="chevron" width={14} height={14} style={{ transform: "rotate(180deg)" }} />
              قبلی
            </button>
            <button className="btn btn-primary" onClick={save} disabled={saving}>
              {saving ? (
                <>
                  <span className="spinner" />
                  {saveNote}
                </>
              ) : (
                <>
                  <Icon name="check" width={14} height={14} />
                  ساخت درس
                </>
              )}
            </button>
          </div>
        </div>
      )}

      {custom.length > 0 && (
        <div className="section">
          <div className="section-head">
            <h2 className="section-title">
              <span className="dot" />
              درس‌های من
            </h2>
            <span className="muted" style={{ fontSize: 12 }}>{custom.length} درس</span>
          </div>
          <div className="panel">
            {custom.map((e) => (
              <div
                key={e.id}
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: 12,
                  padding: "12px 16px",
                  borderBottom: "1px solid var(--line)",
                }}
              >
                <Icon name="headphones" width={16} height={16} />
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div className="en" style={{ fontSize: 13, fontWeight: 700 }}>
                    {e.title}
                  </div>
                  <div className="muted" style={{ fontSize: 11 }}>
                    {e.segments.length} جمله · {fmtTime(e.duration)}
                  </div>
                </div>
                <a className="btn btn-sm btn-outline" href={`#/lesson/${e.id}`}>
                  باز کردن
                </a>
                <button
                  className="btn btn-icon btn-sm btn-ghost"
                  onClick={() => removeCustomEpisode(e.id)}
                  title="حذف"
                >
                  <Icon name="trash" width={14} height={14} />
                </button>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
