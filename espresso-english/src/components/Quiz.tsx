import { useEffect, useMemo, useRef, useState } from "react";
import type { Episode } from "../lib/types";
import { buildQuiz, type Question } from "../lib/quiz";
import { usePlayer, useQuizResults } from "../lib/store";
import { fa } from "../lib/format";
import { Icon } from "./Icon";
import { speak } from "./WordPanel";

export function Quiz({ episode, onSeek }: { episode: Episode; onSeek: (t: number) => void }) {
  const questions = useMemo(() => buildQuiz(episode, 10), [episode]);
  const [started, setStarted] = useState(false);
  const [index, setIndex] = useState(0);
  const [picked, setPicked] = useState<number | null>(null);
  const [checked, setChecked] = useState(false);
  const [score, setScore] = useState(0);
  const [answers, setAnswers] = useState<{ q: Question; picked: number }[]>([]);
  const [done, setDone] = useState(false);
  const { results, save } = useQuizResults();
  const player = usePlayer();
  const savedRef = useRef(false);

  const best = results[episode.id]?.best ?? 0;

  useEffect(() => {
    setStarted(false);
    setIndex(0);
    setPicked(null);
    setChecked(false);
    setScore(0);
    setAnswers([]);
    setDone(false);
    savedRef.current = false;
  }, [episode.id]);

  const q = questions[index];

  const finish = (finalScore: number) => {
    if (!savedRef.current) {
      savedRef.current = true;
      save(episode.id, finalScore, questions.length);
    }
    setDone(true);
  };

  const check = () => {
    if (picked === null || !q) return;
    setChecked(true);
    if (picked === q.answer) setScore((s) => s + 1);
    setAnswers((a) => [...a, { q, picked }]);
  };

  const next = () => {
    if (index + 1 >= questions.length) {
      finish(score);
      return;
    }
    setIndex((i) => i + 1);
    setPicked(null);
    setChecked(false);
  };

  if (!questions.length) {
    return (
      <div className="empty">
        <Icon name="info" width={26} height={26} />
        <p style={{ marginTop: 10 }}>برای این درس هنوز آزمونی ساخته نشده است.</p>
      </div>
    );
  }

  if (!started) {
    return (
      <div className="quiz-intro card">
        <div className="quiz-intro-art">
          <Icon name="quiz" width={30} height={30} />
        </div>
        <h2>آزمون درس «{episode.titleFa}»</h2>
        <p>
          {fa(questions.length)} سؤال از همین قسمت: معنی واژه‌ها، جای خالی، شنیدن جمله و
          عبارت‌های کلیدی. هر بار سؤال‌ها با ترتیب تازه ساخته می‌شوند.
        </p>
        <div className="quiz-types">
          <span className="badge badge-dot" style={{ color: "var(--accent)" }}>معنی واژه</span>
          <span className="badge badge-dot" style={{ color: "var(--c-pron)" }}>جای خالی</span>
          <span className="badge badge-dot" style={{ color: "var(--c-grammar)" }}>شنیداری</span>
          <span className="badge badge-dot" style={{ color: "var(--c-phrases)" }}>عبارت‌ها</span>
        </div>
        {best > 0 && (
          <div className="quiz-best">
            بهترین نتیجه قبلی: <b className="num">{fa(best)}٪</b>
          </div>
        )}
        <button className="btn btn-primary btn-lg" onClick={() => setStarted(true)}>
          <Icon name="play" width={15} height={15} />
          شروع آزمون
        </button>
      </div>
    );
  }

  if (done) {
    const pct = Math.round((score / questions.length) * 100);
    const wrong = answers.filter((a) => a.picked !== a.q.answer);
    return (
      <div className="quiz-result card">
        <div className="score-ring" style={{ ["--pct" as string]: pct }}>
          <svg viewBox="0 0 120 120">
            <circle cx="60" cy="60" r="52" className="ring-bg" />
            <circle
              cx="60"
              cy="60"
              r="52"
              className="ring-fg"
              style={{ strokeDasharray: `${(pct / 100) * 327} 327` }}
            />
          </svg>
          <div className="score-num num">{fa(pct)}٪</div>
        </div>
        <h2>
          {pct >= 80 ? "عالی بود! ☕" : pct >= 50 ? "خوب بود، یک دور دیگر گوش کن" : "بد نبود، دوباره تلاش کن"}
        </h2>
        <p className="muted">
          {fa(score)} از {fa(questions.length)} پاسخ درست
        </p>
        <div className="result-actions">
          <button
            className="btn btn-primary"
            onClick={() => {
              setStarted(true);
              setIndex(0);
              setPicked(null);
              setChecked(false);
              setScore(0);
              setAnswers([]);
              setDone(false);
              savedRef.current = false;
            }}
          >
            <Icon name="repeat" width={14} height={14} />
            آزمون دوباره
          </button>
          <button className="btn btn-outline" onClick={() => onSeek(0)}>
            <Icon name="headphones" width={14} height={14} />
            گوش دادن دوباره
          </button>
        </div>

        {wrong.length > 0 && (
          <div className="wrong-list">
            <div className="side-head">
              <h3>مرور اشتباه‌ها</h3>
            </div>
            {wrong.map(({ q: wq, picked }, i) => (
              <div key={i} className="wrong-item">
                <div className="en" style={{ fontWeight: 700 }}>
                  {wq.kind === "blank" ? wq.sentence : wq.kind === "listen" ? wq.segment.text : wq.term}
                </div>
                <div className="wrong-answers">
                  <span className="bad">✕ {wq.options[picked]}</span>
                  <span className="good">✓ {wq.options[wq.answer]}</span>
                </div>
                {wq.explain && <div className="muted" style={{ fontSize: 12 }}>{wq.explain}</div>}
              </div>
            ))}
          </div>
        )}
      </div>
    );
  }

  const isRight = picked === q.answer;

  return (
    <div className="quiz card">
      <div className="quiz-top">
        <div className="quiz-progress">
          <div className="progress">
            <i style={{ width: `${((index + (checked ? 1 : 0)) / questions.length) * 100}%` }} />
          </div>
          <span className="num">
            {fa(index + 1)} / {fa(questions.length)}
          </span>
        </div>
        <div className="quiz-score num">
          <Icon name="star" width={13} height={13} /> {fa(score)}
        </div>
      </div>

      <div className="quiz-question">
        <div className="quiz-kind">
          {q.kind === "meaning" && "معنی واژه"}
          {q.kind === "blank" && "جای خالی"}
          {q.kind === "listen" && "شنیداری"}
          {q.kind === "phrase" && "عبارت"}
        </div>
        <h3>{q.prompt}</h3>

        {q.kind === "meaning" && (
          <div className="quiz-term">
            <span className="en">{q.term}</span>
            {q.ipa && <span className="num muted">{q.ipa}</span>}
            <button className="btn btn-icon btn-sm btn-ghost" onClick={() => speak(q.term)}>
              <Icon name="volume" width={14} height={14} />
            </button>
          </div>
        )}

        {q.kind === "blank" && <div className="quiz-sentence en">{q.sentence}</div>}

        {q.kind === "phrase" && <div className="quiz-term"><span className="en">{q.term}</span></div>}

        {q.kind === "listen" && (
          <div className="listen-box">
            <button
              className="listen-play"
              onClick={() => {
                player.setRepeat(false);
                player.playRange(q.segment.start, q.segment.end);
              }}
            >
              <Icon name="volume" width={22} height={22} />
            </button>
            <div>
              <div style={{ fontWeight: 700, fontSize: 13.5 }}>جمله را گوش کن</div>
              <div className="muted" style={{ fontSize: 11.5 }}>
                می‌توانی چند بار پخش کنی
              </div>
            </div>
          </div>
        )}

        <div className={`quiz-options${q.kind === "listen" ? " is-en" : ""}`}>
          {q.options.map((opt, i) => {
            const state = !checked
              ? picked === i
                ? "picked"
                : ""
              : i === q.answer
              ? "right"
              : i === picked
              ? "wrong"
              : "dim";
            return (
              <button
                key={i}
                className="quiz-option"
                dir={q.kind === "meaning" ? "rtl" : "ltr"}
                data-state={state}
                disabled={checked}
                onClick={() => setPicked(i)}
              >
                <span className="opt-key num">{["A", "B", "C", "D"][i]}</span>
                <span className={q.kind === "listen" ? "en" : ""}>{opt}</span>
                {checked && i === q.answer && <Icon name="check" width={15} height={15} />}
                {checked && i === picked && i !== q.answer && <Icon name="x" width={15} height={15} />}
              </button>
            );
          })}
        </div>

        {checked && (
          <div className={`quiz-feedback${isRight ? " ok" : " no"}`}>
            <Icon name={isRight ? "check" : "info"} width={15} height={15} />
            <div>
              <b>{isRight ? "درست!" : "پاسخ درست: " + q.options[q.answer]}</b>
              {q.explain && <div style={{ fontSize: 12.5, marginTop: 2 }}>{q.explain}</div>}
            </div>
          </div>
        )}
      </div>

      <div className="quiz-foot">
        {q.kind === "listen" ? (
          <button
            className="btn btn-ghost btn-sm"
            onClick={() => onSeek(q.segment.start)}
          >
            <Icon name="headphones" width={14} height={14} />
            رفتن به این جمله در درس
          </button>
        ) : (
          <span />
        )}
        {!checked ? (
          <button className="btn btn-primary" disabled={picked === null} onClick={check}>
            بررسی پاسخ
          </button>
        ) : (
          <button className="btn btn-primary" onClick={next}>
            {index + 1 >= questions.length ? "دیدن نتیجه" : "سؤال بعدی"}
            <Icon name="arrowLeft" width={14} height={14} />
          </button>
        )}
      </div>
    </div>
  );
}
