import { useEffect, useMemo, useRef, useState } from "react";
import { navigate } from "../App";
import { Icon } from "../components/Icon";
import { Transcript } from "../components/Transcript";
import { WordPanel } from "../components/WordPanel";
import { Quiz } from "../components/Quiz";
import { useData, usePlayer, useProgress, useQuizResults, useSettings, useVocab } from "../lib/store";
import type { Episode, Segment } from "../lib/types";
import {
  categoryLabels,
  fa,
  fmtTime,
  levelLabels,
  levelColors,
} from "../lib/format";

function findSegmentIndex(segments: Segment[], t: number) {
  let lo = 0;
  let hi = segments.length - 1;
  let ans = 0;
  while (lo <= hi) {
    const mid = (lo + hi) >> 1;
    if (segments[mid].start <= t) {
      ans = mid;
      lo = mid + 1;
    } else {
      hi = mid - 1;
    }
  }
  return ans;
}

export function Lesson({ id }: { id: string }) {
  const { getEpisode, episodes } = useData();
  const episode = getEpisode(id);
  const player = usePlayer();
  const { settings, set } = useSettings();
  const { vocab, setVocab } = useVocab();
  const progress = useProgress();
  const { results } = useQuizResults();
  const [tab, setTab] = useState<"transcript" | "quiz">("transcript");
  const [sel, setSel] = useState<{ word: string; segment: Segment; rect: DOMRect } | null>(null);
  const [repeatId, setRepeatId] = useState<string>();
  const [speedOpen, setSpeedOpen] = useState(false);
  const loadedRef = useRef<string | null>(null);

  useEffect(() => {
    if (!episode) return;
    if (loadedRef.current === episode.id && player.episode?.id === episode.id) return;
    loadedRef.current = episode.id;
    player.load(episode, { autoplay: false });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [episode?.id]);

  useEffect(() => {
    setTab("transcript");
    setRepeatId(undefined);
    setSel(null);
  }, [id]);

  const isCurrent = player.episode?.id === episode?.id;
  const time = isCurrent ? player.time : 0;
  const duration = (isCurrent ? player.duration : 0) || episode?.duration || 0;

  const segments = episode?.segments ?? [];
  const activeIndex = useMemo(() => findSegmentIndex(segments, time), [segments, time]);
  const activeSegment = segments[activeIndex];
  const activeWord = useMemo(() => {
    if (!activeSegment) return -1;
    let idx = -1;
    activeSegment.words.forEach((w, i) => {
      if (time >= w.s - 0.02) idx = i;
    });
    return idx;
  }, [activeSegment, time]);

  const savedTerms = useMemo(
    () => new Set(vocab.map((v) => v.term.toLowerCase())),
    [vocab]
  );
  const keyTerms = useMemo(
    () => new Set((episode?.vocab ?? []).map((v) => v.term.toLowerCase())),
    [episode]
  );

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const tag = (e.target as HTMLElement)?.tagName;
      if (tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT") return;
      if (e.code === "Space") {
        e.preventDefault();
        player.toggle();
      } else if (e.key === "ArrowRight") {
        player.skip(5);
      } else if (e.key === "ArrowLeft") {
        player.skip(-5);
      } else if (e.key.toLowerCase() === "r" && activeSegment) {
        toggleRepeat(activeSegment);
      } else if (e.key.toLowerCase() === "t") {
        set("translation", settings.translation === "off" ? "on" : "off");
      } else if (e.key === "Escape") {
        setSel(null);
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [activeSegment, player, settings.translation, set]);

  if (!episode) {
    return (
      <div className="empty">
        <p>این درس پیدا نشد.</p>
        <button className="btn btn-primary" style={{ marginTop: 16 }} onClick={() => navigate("library")}>
          بازگشت به کتابخانه
        </button>
      </div>
    );
  }

  const onSegmentClick = (seg: Segment) => {
    player.setRepeat(false);
    setRepeatId(undefined);
    player.seek(seg.start + 0.02);
    player.play();
  };

  const toggleRepeat = (seg: Segment) => {
    if (repeatId === seg.id) {
      setRepeatId(undefined);
      player.setRepeat(false);
      player.pause();
    } else {
      setRepeatId(seg.id);
      player.setRepeat(true);
      player.playRange(seg.start, seg.end);
    }
  };

  const onWordClick = (word: string, segment: Segment, rect: DOMRect) => {
    player.pause();
    setSel({ word, segment, rect });
  };

  const pct = duration ? (time / duration) * 100 : 0;
  const nextEpisode = episodes[episodes.findIndex((e) => e.id === episode.id) + 1];
  const quiz = results[episode.id];

  return (
    <div className="lesson">
      <div className="player-bar">
        <button
          className="play-main"
          onClick={() => (isCurrent ? player.toggle() : player.load(episode, { autoplay: true }))}
          aria-label={player.playing ? "توقف" : "پخش"}
        >
          <Icon name={player.playing ? "pause" : "play"} width={20} height={20} />
        </button>

        <div className="pb-main">
          <div className="pb-title en">{episode.title}</div>
          <div className="pb-meta">
            <span className="badge" style={{ color: levelColors[episode.level] }}>
              <i className="level-dot" style={{ background: levelColors[episode.level] }} />
              {levelLabels[episode.level]}
            </span>
            <span className="badge">{categoryLabels[episode.category]}</span>
            <span className="muted" style={{ fontSize: 11.5 }}>
              {episode.titleFa}
            </span>
          </div>
        </div>

        <div className="pb-wave">
          <Waveform
            peaks={episode.peaks}
            pct={pct}
            onSeek={(ratio) => {
              if (!isCurrent) player.load(episode, { seek: ratio * duration, autoplay: true });
              else player.seek(ratio * duration);
            }}
          />
          <div className="pb-time num">
            <span>{fmtTime(time)}</span>
            <span className="muted">/ {fmtTime(duration)}</span>
          </div>
        </div>

        <div className="pb-actions">
          <button
            className={`pb-btn${settings.translation !== "off" ? " is-on" : ""}`}
            title="نمایش ترجمه (T)"
            onClick={() => set("translation", settings.translation === "off" ? "on" : "off")}
          >
            <Icon name={settings.translation === "off" ? "eyeOff" : "eye"} width={16} height={16} />
          </button>
          <div style={{ position: "relative" }}>
            <button
              className={`pb-btn${player.repeat ? " is-on" : ""}`}
              title="تکرار جمله (R)"
              onClick={() => {
                player.setRepeat(!player.repeat);
                if (player.repeat) setRepeatId(undefined);
              }}
            >
              <Icon name="repeat" width={16} height={16} />
            </button>
          </div>
          <div style={{ position: "relative" }}>
            <button className="pb-btn" onClick={() => setSpeedOpen((v) => !v)} title="سرعت پخش">
              <span className="num" style={{ fontSize: 12, fontWeight: 700 }}>
                {player.rate}×
              </span>
            </button>
            {speedOpen && (
              <div className="speed-menu" onMouseLeave={() => setSpeedOpen(false)}>
                {[0.6, 0.75, 0.9, 1, 1.15, 1.3].map((r) => (
                  <button
                    key={r}
                    data-active={player.rate === r}
                    onClick={() => {
                      player.setRate(r);
                      set("rate", r);
                      setSpeedOpen(false);
                    }}
                  >
                    <span className="num">{r}×</span>
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="lesson-tabs">
        <button data-active={tab === "transcript"} onClick={() => setTab("transcript")}>
          <Icon name="text" width={15} height={15} /> زیرنویس هوشمند
        </button>
        <button data-active={tab === "quiz"} onClick={() => setTab("quiz")}>
          <Icon name="quiz" width={15} height={15} /> آزمون این درس
          {quiz && <span className="tab-score">{fa(quiz.best)}٪</span>}
        </button>
        <div className="lesson-tabs-info">
          {fa(episode.segments.length)} جمله · {fa(episode.vocab.length)} واژه کلیدی
        </div>
      </div>

      {tab === "transcript" ? (
        <div className="lesson-grid">
          <section className="card transcript-card">
            <Transcript
              episode={episode}
              activeId={activeSegment?.id}
              activeWord={activeWord}
              activeSegment={activeSegment}
              showFa={settings.translation !== "off"}
              blurFa={settings.translation === "blur"}
              autoScroll={settings.autoScroll}
              savedTerms={savedTerms}
              keyTerms={keyTerms}
              repeatId={repeatId}
              onWordClick={onWordClick}
              onSegmentClick={onSegmentClick}
              onRepeat={toggleRepeat}
              playing={player.playing}
            />
            <div className="transcript-foot">
              <span>
                <Icon name="info" width={13} height={13} /> روی هر واژه کلیک کن تا معنی‌اش را ببینی
              </span>
              <span className="kbd-hint">
                <kbd>Space</kbd> پخش · <kbd>R</kbd> تکرار جمله · <kbd>T</kbd> ترجمه
              </span>
            </div>
          </section>

          <aside className="lesson-side">
            <div className="card side-card">
              <div className="side-head">
                <h3>واژه‌های کلیدی</h3>
                <span className="muted">{fa(episode.vocab.length)}</span>
              </div>
              <div className="vocab-list">
                {episode.vocab.map((v) => {
                  const seg = episode.segments.find((s) =>
                    s.words.some(
                      (w) =>
                        w.w.replace(/[^A-Za-z']/g, "").toLowerCase() === v.term.toLowerCase()
                    )
                  );
                  return (
                    <button
                      key={v.term}
                      className="vocab-row"
                      onClick={() => {
                        if (seg) {
                          player.seek(seg.start + 0.02);
                          player.play();
                        }
                      }}
                    >
                      <div>
                        <div className="vocab-term en">{v.term}</div>
                        <div className="vocab-fa">{v.fa}</div>
                      </div>
                      <Icon name="play" width={11} height={11} />
                    </button>
                  );
                })}
              </div>
            </div>

            {episode.phrases && episode.phrases.length > 0 && (
              <div className="card side-card">
                <div className="side-head">
                  <h3>عبارت‌های کاربردی</h3>
                </div>
                <div className="phrase-list">
                  {episode.phrases.map((p) => (
                    <div key={p.en} className="phrase">
                      <div className="en phrase-en">{p.en}</div>
                      <div className="phrase-fa">{p.fa}</div>
                      {p.note && <div className="phrase-note">{p.note}</div>}
                    </div>
                  ))}
                </div>
              </div>
            )}

            <div className="card side-card">
              <div className="side-head">
                <h3>تنظیمات مطالعه</h3>
              </div>
              <div className="setting-row">
                <span>ترجمه فارسی</span>
                <div className="seg-ctrl">
                  {[
                    { v: "off", l: "خاموش" },
                    { v: "on", l: "روشن" },
                    { v: "blur", l: "مخفی" },
                  ].map((o) => (
                    <button
                      key={o.v}
                      data-active={settings.translation === o.v}
                      onClick={() => set("translation", o.v as typeof settings.translation)}
                    >
                      {o.l}
                    </button>
                  ))}
                </div>
              </div>
              <div className="setting-row">
                <span>اسکرول خودکار</span>
                <button
                  className={`switch${settings.autoScroll ? " is-on" : ""}`}
                  onClick={() => set("autoScroll", !settings.autoScroll)}
                  aria-label="اسکرول خودکار"
                >
                  <i />
                </button>
              </div>
              <div className="setting-row">
                <span>اندازه متن</span>
                <input
                  type="range"
                  min={0.85}
                  max={1.5}
                  step={0.05}
                  value={settings.fontScale}
                  onChange={(e) => set("fontScale", Number(e.target.value))}
                />
              </div>
            </div>

            <div className="card side-card">
              <div className="side-head">
                <h3>پیشرفت این درس</h3>
              </div>
              <div className="progress" style={{ marginBottom: 10 }}>
                <i style={{ width: `${Math.min(100, pct)}%` }} />
              </div>
              <div className="side-stats">
                <div>
                  <b className="num">{fa(progress[episode.id]?.plays ?? 0)}</b>
                  <span>بار پخش</span>
                </div>
                <div>
                  <b className="num">{fa(quiz?.best ?? 0)}٪</b>
                  <span>بهترین آزمون</span>
                </div>
                <div>
                  <b className="num">{fa(Math.round((progress[episode.id]?.listenSeconds ?? 0) / 60))}</b>
                  <span>دقیقه گوش</span>
                </div>
              </div>
            </div>
          </aside>
        </div>
      ) : (
        <Quiz episode={episode} onSeek={(t) => {
          player.seek(t);
          player.play();
        }} />
      )}

      {nextEpisode && (
        <div className="next-lesson">
          <div>
            <div className="muted" style={{ fontSize: 12 }}>درس بعدی</div>
            <div className="en" style={{ fontWeight: 700, marginTop: 2 }}>{nextEpisode.title}</div>
          </div>
          <button className="btn btn-outline" onClick={() => navigate(`lesson/${nextEpisode.id}`)}>
            برو به درس بعدی
            <Icon name="arrowLeft" width={15} height={15} />
          </button>
        </div>
      )}

      {sel && (
        <WordPanel
          word={sel.word}
          episode={episode}
          segment={sel.segment}
          anchor={sel.rect}
          onClose={() => setSel(null)}
        />
      )}
    </div>
  );
}

function Waveform({
  peaks,
  pct,
  onSeek,
}: {
  peaks: number[];
  pct: number;
  onSeek: (ratio: number) => void;
}) {
  const ref = useRef<HTMLDivElement>(null);
  const [hover, setHover] = useState<number | null>(null);
  const bars = peaks.length ? peaks : Array.from({ length: 60 }, () => 30);
  return (
    <div
      className="wave"
      ref={ref}
      onMouseMove={(e) => {
        const rect = ref.current!.getBoundingClientRect();
        setHover((e.clientX - rect.left) / rect.width);
      }}
      onMouseLeave={() => setHover(null)}
      onClick={(e) => {
        const rect = ref.current!.getBoundingClientRect();
        onSeek(Math.min(1, Math.max(0, (e.clientX - rect.left) / rect.width)));
      }}
    >
      {bars.map((h, i) => {
        const barPct = (i / bars.length) * 100;
        const played = barPct <= pct;
        return (
          <i
            key={i}
            style={{
              height: `${Math.max(8, h)}%`,
              background: played ? "var(--accent)" : "var(--surface-3)",
              opacity: played ? 1 : hover !== null && barPct <= hover * 100 ? 0.85 : 0.65,
            }}
          />
        );
      })}
    </div>
  );
}
