import { useMemo, useState } from "react";
import { Icon } from "../components/Icon";
import { speak } from "../components/WordPanel";
import { useData, useQuizResults, useVocab } from "../lib/store";
import { dueWords, reviewWord } from "../lib/storage";
import { fa, relativeFa, shuffle } from "../lib/format";
import type { SavedWord } from "../lib/types";

export function Review() {
  const { vocab, setVocab } = useVocab();
  const { results } = useQuizResults();
  const { episodes } = useData();
  const [mode, setMode] = useState<"flash" | "quiz" | null>(null);

  const due = useMemo(() => dueWords(vocab), [vocab]);
  const mastered = vocab.filter((w) => w.box >= 4).length;
  const recent = Object.entries(results)
    .map(([id, r]) => ({ id, r, ep: episodes.find((e) => e.id === id) }))
    .filter((x) => x.ep)
    .sort((a, b) => b.r.lastAt - a.r.lastAt)
    .slice(0, 5);

  return (
    <div>
      <div className="review-head">
        <h1>آزمون و مرور</h1>
        <p>مرور فاصله‌دار واژه‌ها + آزمون‌های درس‌ها، همه در یک جا.</p>
      </div>

      <div className="review-grid">
        <div className="panel review-panel">
          <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 6 }}>
            <div className="stat-icon" style={{ margin: 0 }}>
              <Icon name="brain" width={16} height={16} />
            </div>
            <h3 style={{ margin: 0 }}>فلش‌کارت واژه‌ها</h3>
          </div>
          <div className="review-big">{fa(due.length)}</div>
          <p>
            واژه‌ی آماده‌ی مرور امروز. کارت را ببین، معنی را حدس بزن، بعد جواب را ببین.
            واژه‌هایی که نمی‌دانی فردا دوباره برمی‌گردند.
          </p>
          <div style={{ display: "flex", gap: 8, marginTop: 16, flexWrap: "wrap" }}>
            <button
              className="btn btn-primary"
              disabled={!vocab.length}
              onClick={() => setMode("flash")}
            >
              <Icon name="play" width={14} height={14} />
              شروع مرور
            </button>
            <a className="btn btn-outline" href="#/notebook">
              <Icon name="notebook" width={14} height={14} />
              واژه‌نامه
            </a>
          </div>
          {!vocab.length && (
            <p style={{ marginTop: 12, fontSize: 11.5 }}>
              هنوز واژه‌ای ذخیره نکرده‌ای. در درس، روی کلمه‌ها بزن و ذخیره کن.
            </p>
          )}
        </div>

        <div className="panel review-panel">
          <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 6 }}>
            <div className="stat-icon" style={{ margin: 0, background: "var(--good-soft)", color: "var(--good)" }}>
              <Icon name="quiz" width={16} height={16} />
            </div>
            <h3 style={{ margin: 0 }}>آزمون واژه‌ها</h3>
          </div>
          <div className="review-big">{fa(vocab.length)}</div>
          <p>
            یک آزمون چهارگزینه‌ای از واژه‌های ذخیره‌شده‌ات؛ برای وقتی که می‌خواهی
            محک بزنی چقدر در ذهنت مانده.
          </p>
          <button
            className="btn btn-outline"
            style={{ marginTop: 16 }}
            disabled={vocab.length < 4}
            onClick={() => setMode("quiz")}
          >
            <Icon name="target" width={14} height={14} />
            {vocab.length < 4 ? "حداقل ۴ واژه لازم است" : "شروع آزمون"}
          </button>
        </div>
      </div>

      <div className="section">
        <div className="section-head">
          <h2 className="section-title">
            <span className="dot" />
            آخرین آزمون‌های درس‌ها
          </h2>
        </div>
        {recent.length ? (
          <div className="panel" style={{ overflow: "hidden" }}>
            {recent.map(({ id, r, ep }) => (
              <a
                key={id}
                href={`#/lesson/${id}`}
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: 14,
                  padding: "14px 18px",
                  borderBottom: "1px solid var(--line)",
                }}
              >
                <div
                  className="stat-icon"
                  style={{ margin: 0, background: "var(--surface-2)", color: "var(--accent)" }}
                >
                  <Icon name="headphones" width={15} height={15} />
                </div>
                <div style={{ minWidth: 0, flex: 1 }}>
                  <div className="en" style={{ fontSize: 13, fontWeight: 700 }}>
                    {ep!.title}
                  </div>
                  <div className="muted" style={{ fontSize: 11 }}>
                    {fa(r.attempts)} بار آزمون · {relativeFa(r.lastAt)}
                  </div>
                </div>
                <div style={{ textAlign: "center" }}>
                  <div className="num" style={{ fontSize: 18, fontWeight: 800, color: "var(--accent)" }}>
                    {fa(r.best)}٪
                  </div>
                  <div className="muted" style={{ fontSize: 10 }}>بهترین</div>
                </div>
                <Icon name="chevron" width={14} height={14} style={{ transform: "rotate(180deg)", color: "var(--muted)" }} />
              </a>
            ))}
          </div>
        ) : (
          <div className="empty">
            <Icon name="quiz" width={24} height={24} />
            <p style={{ marginTop: 8 }}>هنوز آزمونی نداده‌ای. از هر درس، تب «آزمون این درس» را ببین.</p>
          </div>
        )}
      </div>

      <div className="section">
        <div className="stats-grid">
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
            <div className="stat-icon"><Icon name="flame" width={16} height={16} /></div>
            <div className="stat-value">{fa(vocab.filter((w) => w.box >= 2).length)}</div>
            <div className="stat-label">در حال تثبیت</div>
          </div>
        </div>
      </div>

      {mode === "flash" && (
        <Flashcards
          deck={shuffle(due.length ? due : vocab, Date.now() % 1000)}
          onClose={() => setMode(null)}
          onAnswer={(term, knew) => setVocab(reviewWord(vocab, term, knew))}
        />
      )}
      {mode === "quiz" && (
        <WordQuiz words={shuffle(vocab, 7)} onClose={() => setMode(null)} />
      )}
    </div>
  );
}

function Flashcards({
  deck,
  onClose,
  onAnswer,
}: {
  deck: SavedWord[];
  onClose: () => void;
  onAnswer: (term: string, knew: boolean) => void;
}) {
  const [i, setI] = useState(0);
  const [shown, setShown] = useState(false);
  const [stats, setStats] = useState({ known: 0, unknown: 0 });
  const done = i >= deck.length;
  const card = deck[i];

  const answer = (knew: boolean) => {
    onAnswer(card.term, knew);
    setStats((s) => ({ known: s.known + (knew ? 1 : 0), unknown: s.unknown + (knew ? 0 : 1) }));
    setShown(false);
    setI((v) => v + 1);
  };

  return (
    <div className="flash-overlay" onClick={onClose}>
      <div className="flash-card" onClick={(e) => e.stopPropagation()}>
        <button className="btn btn-icon btn-sm btn-ghost flash-close" onClick={onClose}>
          <Icon name="x" width={16} height={16} />
        </button>
        {!done ? (
          <>
            <div className="flash-progress">
              <div className="progress" style={{ flex: 1, margin: 0 }}>
                <i style={{ width: `${(i / deck.length) * 100}%` }} />
              </div>
              <span className="num">
                {fa(i + 1)} / {fa(deck.length)}
              </span>
            </div>
            <div className="flash-term en">{card.term}</div>
            {card.ipa && <div className="flash-ipa">{card.ipa}</div>}
            <button className="btn btn-icon btn-sm btn-ghost" onClick={() => speak(card.term)}>
              <Icon name="volume" width={15} height={15} />
            </button>

            {shown ? (
              <>
                <div className="flash-fa">{card.fa}</div>
                {card.en && <div className="flash-en">{card.en}</div>}
                {card.example && <div className="flash-en">“{card.example}”</div>}
                <div className="flash-actions">
                  <button className="btn" onClick={() => answer(false)}>
                    <Icon name="x" width={14} height={14} />
                    نمی‌دانستم
                  </button>
                  <button className="btn btn-primary" onClick={() => answer(true)}>
                    <Icon name="check" width={14} height={14} />
                    می‌دانستم
                  </button>
                </div>
              </>
            ) : (
              <div className="flash-actions">
                <button className="btn btn-primary btn-lg" onClick={() => setShown(true)}>
                  <Icon name="eye" width={16} height={16} />
                  نمایش معنی
                </button>
              </div>
            )}
          </>
        ) : (
          <>
            <div className="stat-icon" style={{ width: 54, height: 54, borderRadius: 18 }}>
              <Icon name="check" width={26} height={26} />
            </div>
            <h2 style={{ fontSize: 22 }}>مرور تمام شد</h2>
            <p className="muted">
              {fa(stats.known)} واژه را می‌دانستی و {fa(stats.unknown)} واژه را نه.
            </p>
            <div className="flash-actions">
              <button className="btn btn-primary" onClick={onClose}>
                بستن
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

function WordQuiz({ words, onClose }: { words: SavedWord[]; onClose: () => void }) {
  const pool = words.slice(0, 8);
  const [i, setI] = useState(0);
  const [picked, setPicked] = useState<number | null>(null);
  const [score, setScore] = useState(0);
  const [done, setDone] = useState(false);

  const card = pool[i];
  const options = useMemo(() => {
    if (!card) return [];
    const others = shuffle(
      words.filter((w) => w.term !== card.term).map((w) => w.fa),
      i + 3
    ).slice(0, 3);
    return shuffle([card.fa, ...others], i + 11);
  }, [card, i, words]);

  if (done) {
    const pct = Math.round((score / pool.length) * 100);
    return (
      <div className="flash-overlay" onClick={onClose}>
        <div className="flash-card" onClick={(e) => e.stopPropagation()}>
          <div className="review-big" style={{ fontSize: 52 }}>{fa(pct)}٪</div>
          <h2 style={{ fontSize: 21 }}>
            {pct >= 80 ? "واژه‌ها خوب جا افتاده‌اند" : "یک دور دیگر مرور کن"}
          </h2>
          <p className="muted">
            {fa(score)} از {fa(pool.length)} پاسخ درست
          </p>
          <div className="flash-actions">
            <button className="btn btn-primary" onClick={onClose}>
              بستن
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="flash-overlay" onClick={onClose}>
      <div className="flash-card" style={{ alignItems: "stretch", textAlign: "start" }} onClick={(e) => e.stopPropagation()}>
        <div className="flash-progress">
          <div className="progress" style={{ flex: 1, margin: 0 }}>
            <i style={{ width: `${(i / pool.length) * 100}%` }} />
          </div>
          <span className="num">
            {fa(i + 1)} / {fa(pool.length)}
          </span>
        </div>
        <div className="quiz-kind">معنی واژه</div>
        <div className="flash-term en" style={{ fontSize: 34 }}>{card.term}</div>
        <div className="quiz-options" style={{ gridTemplateColumns: "1fr", marginTop: 12 }}>
          {options.map((opt, idx) => {
            const isRight = opt === card.fa;
            const state =
              picked === null
                ? ""
                : isRight
                ? "right"
                : picked === idx
                ? "wrong"
                : "dim";
            return (
              <button
                key={idx}
                className="quiz-option"
                data-state={state}
                disabled={picked !== null}
                onClick={() => {
                  setPicked(idx);
                  if (isRight) setScore((s) => s + 1);
                  setTimeout(() => {
                    if (i + 1 >= pool.length) setDone(true);
                    else {
                      setI(i + 1);
                      setPicked(null);
                    }
                  }, 700);
                }}
              >
                <span className="opt-key num">{["A", "B", "C", "D"][idx]}</span>
                <span>{opt}</span>
                {picked !== null && isRight && <Icon name="check" width={15} height={15} />}
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}
