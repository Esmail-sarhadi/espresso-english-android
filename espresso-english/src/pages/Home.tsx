import { useMemo } from "react";
import { navigate } from "../App";
import { Icon } from "../components/Icon";
import { useData, usePlayer, useProgress, useQuizResults, useVocab } from "../lib/store";
import { categoryGradient, categoryLabels, fa, fmtMinutes, fmtTime, levelLabels } from "../lib/format";
import type { Episode } from "../lib/types";

export function Home() {
  const { episodes, podcasts } = useData();
  const progress = useProgress();
  const { vocab } = useVocab();
  const { results } = useQuizResults();
  const player = usePlayer();

  const continueEp = useMemo(() => {
    const entries = Object.entries(progress)
      .filter(([, p]) => p.position > 3 && p.position < p.duration - 2)
      .sort((a, b) => b[1].lastAt - a[1].lastAt);
    if (!entries.length) return null;
    const ep = episodes.find((e) => e.id === entries[0][0]);
    return ep ? { ep, p: entries[0][1] } : null;
  }, [episodes, progress]);

  const listenMinutes = Math.round(
    Object.values(progress).reduce((sum, p) => sum + (p.listenSeconds ?? 0), 0) / 60
  );
  const completed = Object.values(progress).filter((p) => p.completed).length;
  const avgQuiz = (() => {
    const vals = Object.values(results).map((r) => r.best);
    if (!vals.length) return 0;
    return Math.round(vals.reduce((a, b) => a + b, 0) / vals.length);
  })();

  const featured = episodes.slice(0, 3);
  const heroEp = episodes[0];

  return (
    <div>
      <section className="hero fade-up">
        <div className="hero-content">
          <div className="eyebrow">
            <span className="pulse" />
            Espresso English Podcast · زیرنویس واژه‌به‌واژه
          </div>
          <h1>
            انگلیسی را جرعه‌جرعه،
            <br />
            مثل یک <span className="accent">اسپرسوی تازه</span>
          </h1>
          <p className="lead">
            هر قسمت را با زیرنویس همگام گوش بده، روی هر واژه بزن تا معنی‌اش را ببینی، جمله را
            قفل کن و تکرار کن، و آخر درس با یک آزمون کوتاه مطمئن شو که یاد گرفته‌ای.
          </p>
          <div className="hero-actions">
            <button
              className="btn btn-primary btn-lg"
              onClick={() => {
                if (heroEp) {
                  player.load(heroEp, { autoplay: true });
                  navigate(`lesson/${heroEp.id}`);
                }
              }}
            >
              <Icon name="play" width={16} height={16} />
              شروع یادگیری
            </button>
            <button className="btn btn-outline btn-lg" onClick={() => navigate("library")}>
              <Icon name="library" width={16} height={16} />
              همه درس‌ها
            </button>
          </div>
          <div className="hero-stats">
            <div className="hero-stat">
              <b>{fa(episodes.length)}</b>
              <span>درس آماده</span>
            </div>
            <div className="hero-stat">
              <b>{fa(Math.round(episodes.reduce((s, e) => s + e.duration, 0) / 60))}</b>
              <span>دقیقه صدا</span>
            </div>
            <div className="hero-stat">
              <b>{fa(episodes.reduce((s, e) => s + e.vocab.length, 0))}</b>
              <span>واژه با معنی</span>
            </div>
          </div>
        </div>
        <div className="hero-visual">
          <div className="hero-glow" />
          <CupArt />
          <div className="float-chip">
            <i>brew</i> دم کردن
          </div>
          <div className="float-chip">
            <i>aroma</i> عطر
          </div>
          <div className="float-chip">
            <i>bitter</i> تلخ
          </div>
        </div>
      </section>

      {continueEp && (
        <section className="section">
          <div className="section-head">
            <h2 className="section-title">
              <span className="dot" />
              از همان‌جا که بودی
            </h2>
          </div>
          <div
            className="continue-card"
            onClick={() => navigate(`lesson/${continueEp.ep.id}`)}
            role="button"
            tabIndex={0}
          >
            <div
              className="continue-art"
              style={{ background: categoryGradient(continueEp.ep.category) }}
            >
              <Icon name="headphones" />
            </div>
            <div className="continue-body">
              <div className="continue-title">{continueEp.ep.title}</div>
              <div className="continue-meta">
                {continueEp.ep.titleFa} · {categoryLabels[continueEp.ep.category]} ·{" "}
                {levelLabels[continueEp.ep.level]}
              </div>
              <div className="progress">
                <i
                  style={{
                    width: `${Math.min(
                      100,
                      (continueEp.p.position / (continueEp.p.duration || continueEp.ep.duration)) * 100
                    )}%`,
                  }}
                />
              </div>
              <div className="continue-meta num" style={{ marginTop: 6 }}>
                {fmtTime(continueEp.p.position)} از {fmtTime(continueEp.p.duration || continueEp.ep.duration)}
              </div>
            </div>
            <button className="btn btn-primary">
              <Icon name="play" width={14} height={14} />
              ادامه
            </button>
          </div>
        </section>
      )}

      <section className="section">
        <div className="section-head">
          <h2 className="section-title">
            <span className="dot" />
            تازه‌ترین درس‌ها
          </h2>
          <a className="section-more" href="#/library">
            همه درس‌ها
            <Icon name="chevron" width={12} height={12} style={{ transform: "rotate(180deg)" }} />
          </a>
        </div>
        <div className="grid-episodes">
          {featured.map((ep, i) => (
            <EpisodeCard key={ep.id} episode={ep} index={i} />
          ))}
        </div>
      </section>

      <section className="section">
        <div className="section-head">
          <h2 className="section-title">
            <span className="dot" />
            کارنامه تو
          </h2>
        </div>
        <div className="stats-grid">
          <Stat icon="bookmark" value={fa(vocab.length)} label="واژه ذخیره‌شده" />
          <Stat icon="headphones" value={fa(listenMinutes)} label="دقیقه گوش دادن" />
          <Stat icon="check" value={fa(completed)} label="درس تمام‌شده" />
          <Stat icon="target" value={`${fa(avgQuiz)}٪`} label="میانگین آزمون" />
        </div>
      </section>

      <section className="section">
        <div className="section-head">
          <h2 className="section-title">
            <span className="dot" />
            چطور کار می‌کند؟
          </h2>
        </div>
        <div className="steps">
          <div className="step">
            <div className="step-index">01</div>
            <h4>گوش بده و بخوان</h4>
            <p>
              زیرنویس انگلیسی دقیقاً با صدا همگام است؛ واژه‌ی در حال پخش روشن می‌شود و صفحه
              خودش همراهت می‌آید.
            </p>
          </div>
          <div className="step">
            <div className="step-index">02</div>
            <h4>روی واژه بزن</h4>
            <p>
              پخش را نگه دار، روی هر کلمه کلیک کن: معنی فارسی، تلفظ، تعریف انگلیسی و جمله‌ی
              نمونه. هر واژه را می‌توانی به واژه‌نامه‌ات اضافه کنی.
            </p>
          </div>
          <div className="step">
            <div className="step-index">03</div>
            <h4>آزمون بده</h4>
            <p>
              در پایان هر درس، آزمون همان قسمت را بده: معنی واژه، جای خالی، شنیداری و عبارت‌ها —
              با بازخورد فوری.
            </p>
          </div>
        </div>
      </section>

      <section className="section">
        <div className="section-head">
          <h2 className="section-title">
            <span className="dot" />
            پادکست‌های دیگر برای یادگیری
          </h2>
          <a className="section-more" href="#/explore">
            دیدن همه
            <Icon name="chevron" width={12} height={12} style={{ transform: "rotate(180deg)" }} />
          </a>
        </div>
        <div className="pod-grid">
          {podcasts.slice(0, 3).map((p) => (
            <div key={p.name} className="pod-card">
              <div className="pod-top">
                <div
                  className="pod-avatar"
                  style={{ background: "var(--accent-soft)", color: "var(--accent)" }}
                >
                  {p.name.slice(0, 1)}
                </div>
                <div>
                  <div className="pod-name">{p.name}</div>
                  <div className="pod-host">{p.host}</div>
                </div>
                <span className="badge" style={{ marginInlineStart: "auto" }}>
                  {levelLabels[p.level]}
                </span>
              </div>
              <p className="pod-desc">{p.descriptionFa}</p>
              <a className="pod-link" href={p.url} target="_blank" rel="noreferrer">
                سایت پادکست
                <Icon name="external" width={12} height={12} />
              </a>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}

function Stat({ icon, value, label }: { icon: "bookmark" | "headphones" | "check" | "target"; value: string; label: string }) {
  return (
    <div className="stat-card">
      <div className="stat-icon">
        <Icon name={icon} width={16} height={16} />
      </div>
      <div className="stat-value">{value}</div>
      <div className="stat-label">{label}</div>
    </div>
  );
}

export function EpisodeCard({ episode, index = 0 }: { episode: Episode; index?: number }) {
  const progress = useProgress();
  const p = progress[episode.id];
  const pct = p && p.duration ? Math.min(100, (p.position / p.duration) * 100) : 0;
  return (
    <a
      href={`#/lesson/${episode.id}`}
      className="ep-card fade-up"
      style={{ animationDelay: `${index * 40}ms` }}
    >
      <div className="ep-cover" style={{ background: categoryGradient(episode.category) }}>
        <span className="ep-cover-badge">{categoryLabels[episode.category]}</span>
        <span className="ep-play">
          <Icon name="play" width={15} height={15} />
        </span>
      </div>
      <div className="ep-body">
        <div className="ep-title">{episode.title}</div>
        <div className="ep-title-fa">{episode.titleFa}</div>
        <div className="ep-foot">
          <span className="level-dot" style={{ color: levelColors(episode.level) }}>
            {levelLabels[episode.level]}
          </span>
          <span className="sep" />
          <span>{fmtMinutes(episode.duration)}</span>
          <span className="sep" />
          <span>{fa(episode.vocab.length)} واژه</span>
          {pct > 0 && (
            <span style={{ marginInlineStart: "auto", color: "var(--accent)" }}>
              {fa(Math.round(pct))}٪
            </span>
          )}
        </div>
      </div>
    </a>
  );
}

function levelColors(level: string) {
  return level === "beginner" ? "var(--good)" : level === "advanced" ? "var(--bad)" : "var(--accent)";
}

function CupArt() {
  return (
    <svg className="cup" viewBox="0 0 200 200" fill="none" aria-hidden="true">
      <defs>
        <linearGradient id="cupBody" x1="60" y1="70" x2="140" y2="170" gradientUnits="userSpaceOnUse">
          <stop stopColor="#F7E9D3" />
          <stop offset="1" stopColor="#D8B98E" />
        </linearGradient>
        <linearGradient id="coffee" x1="80" y1="90" x2="120" y2="110" gradientUnits="userSpaceOnUse">
          <stop stopColor="#6B4326" />
          <stop offset="1" stopColor="#2A1810" />
        </linearGradient>
      </defs>
      <g className="steam">
        <path d="M82 66c-8-10 8-14 0-24" />
        <path d="M100 62c-8-12 8-16 0-28" />
        <path d="M118 66c-8-10 8-14 0-24" />
      </g>
      <path
        d="M52 88h96v34c0 22-18 40-40 40h-16c-22 0-40-18-40-40z"
        fill="url(#cupBody)"
      />
      <path d="M148 96h12a16 16 0 0 1 0 32h-12" stroke="#E4CBA6" strokeWidth="7" strokeLinecap="round" />
      <ellipse cx="100" cy="90" rx="48" ry="11" fill="url(#coffee)" />
      <ellipse cx="100" cy="88" rx="40" ry="7" fill="#C69A63" opacity="0.55" />
      <path d="M66 158h68" stroke="#C9A87C" strokeWidth="6" strokeLinecap="round" />
    </svg>
  );
}
