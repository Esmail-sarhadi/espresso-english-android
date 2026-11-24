import { useMemo, useState } from "react";
import { Icon } from "../components/Icon";
import { speak } from "../components/WordPanel";
import { useVocab } from "../lib/store";
import { reviewWord, dueWords } from "../lib/storage";
import { fa } from "../lib/format";
import type { SavedWord } from "../lib/types";

export function Notebook() {
  const { vocab, setVocab } = useVocab();
  const [q, setQ] = useState("");
  const [filter, setFilter] = useState<"all" | "due" | "mastered">("all");
  const [open, setOpen] = useState<string | null>(null);

  const due = useMemo(() => dueWords(vocab), [vocab]);

  const list = useMemo(() => {
    let out = vocab;
    if (filter === "due") out = due;
    if (filter === "mastered") out = vocab.filter((w) => w.box >= 4);
    if (q.trim()) {
      const n = q.trim().toLowerCase();
      out = out.filter(
        (w) =>
          w.term.toLowerCase().includes(n) ||
          w.fa.includes(q.trim()) ||
          (w.en ?? "").toLowerCase().includes(n)
      );
    }
    return [...out].sort((a, b) => b.createdAt - a.createdAt);
  }, [vocab, filter, q, due]);

  const mastered = vocab.filter((w) => w.box >= 4).length;

  const exportJson = () => {
    const blob = new Blob([JSON.stringify(vocab, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "espresso-vocab.json";
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div>
      <div className="page-head">
        <div>
          <h1 className="page-title">واژه‌نامه من</h1>
          <p className="page-sub">
            هر واژه‌ای که ذخیره کرده‌ای، با مرور فاصله‌دار برمی‌گردد تا فراموش نشود.
          </p>
        </div>
        <div style={{ marginInlineStart: "auto", display: "flex", gap: 8 }}>
          <button className="btn btn-outline btn-sm" onClick={exportJson} disabled={!vocab.length}>
            <Icon name="download" width={14} height={14} />
            خروجی JSON
          </button>
          <a className="btn btn-primary btn-sm" href="#/review">
            <Icon name="sparkles" width={14} height={14} />
            مرور امروز
          </a>
        </div>
      </div>

      <div className="stats-grid" style={{ marginBottom: 26 }}>
        <div className="stat-card">
          <div className="stat-icon"><Icon name="bookmark" width={16} height={16} /></div>
          <div className="stat-value">{fa(vocab.length)}</div>
          <div className="stat-label">کل واژه‌ها</div>
        </div>
        <div className="stat-card">
          <div className="stat-icon" style={{ background: "var(--bad-soft)", color: "var(--bad)" }}>
            <Icon name="clock" width={16} height={16} />
          </div>
          <div className="stat-value">{fa(due.length)}</div>
          <div className="stat-label">آماده مرور</div>
        </div>
        <div className="stat-card">
          <div className="stat-icon" style={{ background: "var(--good-soft)", color: "var(--good)" }}>
            <Icon name="check" width={16} height={16} />
          </div>
          <div className="stat-value">{fa(mastered)}</div>
          <div className="stat-label">مسلط</div>
        </div>
        <div className="stat-card">
          <div className="stat-icon">
            <Icon name="target" width={16} height={16} />
          </div>
          <div className="stat-value">
            {fa(
              vocab.length
                ? Math.round(
                    (vocab.reduce((s, w) => s + w.correct, 0) /
                      Math.max(1, vocab.reduce((s, w) => s + w.seen, 0))) *
                      100
                  )
                : 0
            )}
            ٪
          </div>
          <div className="stat-label">دقت مرور</div>
        </div>
      </div>

      <div className="filters">
        {[
          { k: "all", l: "همه" },
          { k: "due", l: `آماده مرور (${fa(due.length)})` },
          { k: "mastered", l: "مسلط" },
        ].map((f) => (
          <button
            key={f.k}
            className="chip"
            data-active={filter === f.k}
            onClick={() => setFilter(f.k as typeof filter)}
          >
            {f.l}
          </button>
        ))}
        <div className="search" style={{ marginInlineStart: "auto" }}>
          <Icon name="search" />
          <input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="جست‌وجو در واژه‌ها…"
          />
        </div>
      </div>

      {list.length ? (
        <div className="word-grid">
          {list.map((w) => (
            <WordCard
              key={w.term}
              word={w}
              open={open === w.term}
              onToggle={() => setOpen(open === w.term ? null : w.term)}
              onDelete={() => setVocab(vocab.filter((x) => x.term !== w.term))}
              onReview={(knew) => setVocab(reviewWord(vocab, w.term, knew))}
            />
          ))}
        </div>
      ) : (
        <div className="empty">
          <Icon name="notebook" width={26} height={26} />
          <p style={{ marginTop: 10 }}>
            {vocab.length
              ? "با این فیلتر واژه‌ای نیست."
              : "هنوز واژه‌ای ذخیره نکرده‌ای. در درس، روی هر کلمه بزن و «افزودن به واژه‌نامه» را انتخاب کن."}
          </p>
          <a className="btn btn-primary btn-sm" href="#/library" style={{ marginTop: 14 }}>
            رفتن به کتابخانه
          </a>
        </div>
      )}
    </div>
  );
}

function WordCard({
  word,
  open,
  onToggle,
  onDelete,
  onReview,
}: {
  word: SavedWord;
  open: boolean;
  onToggle: () => void;
  onDelete: () => void;
  onReview: (knew: boolean) => void;
}) {
  return (
    <div className={`word-card${open ? " is-open" : ""}`} onClick={onToggle}>
      <div className="wc-top">
        <div>
          <div className="wc-term en">{word.term}</div>
          {word.ipa && <div className="wc-ipa num">{word.ipa}</div>}
        </div>
        <button
          className="btn btn-icon btn-sm btn-ghost"
          onClick={(e) => {
            e.stopPropagation();
            speak(word.term);
          }}
        >
          <Icon name="volume" width={14} height={14} />
        </button>
      </div>
      <div className="wc-fa">{word.fa}</div>
      {open && (
        <div className="wc-detail">
          {word.en && <p className="en">{word.en}</p>}
          {word.example && <p className="en wc-example">“{word.example}”</p>}
          {word.episodeTitle && (
            <a className="wc-source" href={`#/lesson/${word.episodeId}`}>
              <Icon name="headphones" width={12} height={12} />
              {word.episodeTitle}
            </a>
          )}
          <div className="wc-actions">
            <button
              className="btn btn-sm"
              onClick={(e) => {
                e.stopPropagation();
                onReview(false);
              }}
            >
              <Icon name="x" width={12} height={12} />
              نمی‌دانستم
            </button>
            <button
              className="btn btn-sm btn-primary"
              onClick={(e) => {
                e.stopPropagation();
                onReview(true);
              }}
            >
              <Icon name="check" width={12} height={12} />
              می‌دانستم
            </button>
            <button
              className="btn btn-icon btn-sm btn-ghost"
              style={{ marginInlineStart: "auto" }}
              onClick={(e) => {
                e.stopPropagation();
                onDelete();
              }}
            >
              <Icon name="trash" width={13} height={13} />
            </button>
          </div>
        </div>
      )}
      <div className="wc-foot">
        <div className="box-dots">
          {[0, 1, 2, 3, 4, 5].map((b) => (
            <i key={b} data-on={b <= word.box} />
          ))}
        </div>
        <span className="muted">
          {word.due <= Date.now()
            ? "آماده مرور"
            : `مرور در ${fa(Math.max(1, Math.ceil((word.due - Date.now()) / 86400000)))} روز`}
        </span>
      </div>
    </div>
  );
}
