import { useMemo, useState } from "react";
import { useData } from "../lib/store";
import { Icon } from "../components/Icon";
import { fa, levelLabels } from "../lib/format";
import type { Level } from "../lib/types";

export function Explore() {
  const { podcasts } = useData();
  const [level, setLevel] = useState<Level | "all">("all");
  const [q, setQ] = useState("");

  const list = useMemo(() => {
    return podcasts.filter((p) => {
      if (level !== "all" && p.level !== level) return false;
      if (q.trim()) {
        const n = q.trim().toLowerCase();
        return (
          p.name.toLowerCase().includes(n) ||
          p.host.toLowerCase().includes(n) ||
          p.descriptionFa.includes(q.trim()) ||
          p.tags.some((t) => t.includes(q.trim()))
        );
      }
      return true;
    });
  }, [podcasts, level, q]);

  return (
    <div>
      <div className="explore-hero">
        <div style={{ flex: 1 }}>
          <h1>پادکست‌های دیگر برای یادگیری</h1>
          <p>
            این‌ها منابعی هستند که خودم برای یادگیری انگلیسی پیشنهاد می‌کنم؛ از کوتاه و ساده
            تا پیشرفته و طبیعی. هر کدام لینک رسمی خودش را دارد و محتوایشان خارج از این
            سایت است. اگر فایل صوتی و متن یک قسمت را داشته باشی، از صفحه‌ی «افزودن درس»
            می‌توانی همین زیرنویس هوشمند و آزمون را برایش بسازی.
          </p>
        </div>
        <div className="explore-icon">
          <Icon name="radio" width={34} height={34} />
        </div>
      </div>

      <div className="filters">
        {[
          { k: "all", l: "همه" },
          { k: "beginner", l: "مبتدی" },
          { k: "intermediate", l: "متوسط" },
          { k: "advanced", l: "پیشرفته" },
        ].map((f) => (
          <button
            key={f.k}
            className="chip"
            data-active={level === f.k}
            onClick={() => setLevel(f.k as Level | "all")}
          >
            {f.l}
          </button>
        ))}
        <div className="search" style={{ marginInlineStart: "auto" }}>
          <Icon name="search" />
          <input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="جست‌وجوی پادکست…"
          />
        </div>
        <span className="result-count">{fa(list.length)} پادکست</span>
      </div>

      <div className="pod-grid">
        {list.map((p) => (
          <div key={p.name} className="pod-card">
            <div className="pod-top">
              <div
                className="pod-avatar"
                style={{
                  background: "linear-gradient(145deg, var(--surface-3), var(--surface-2))",
                  color: "var(--accent)",
                  border: "1px solid var(--line)",
                }}
              >
                {p.name.slice(0, 1)}
              </div>
              <div style={{ minWidth: 0 }}>
                <div className="pod-name">{p.name}</div>
                <div className="pod-host">
                  {p.host} · {p.accent}
                </div>
              </div>
              <div style={{ marginInlineStart: "auto", display: "flex", gap: 4, flexDirection: "column", alignItems: "flex-end" }}>
                <span className="badge">{levelLabels[p.level]}</span>
                {p.badge && (
                  <span className="badge" style={{ background: "var(--accent-soft)", color: "var(--accent)" }}>
                    {p.badge}
                  </span>
                )}
              </div>
            </div>
            <p className="pod-desc">{p.descriptionFa}</p>
            <div className="pod-tags">
              {p.tags.map((t) => (
                <span key={t} className="pod-tag">
                  {t}
                </span>
              ))}
            </div>
            <p className="pod-why">
              <b style={{ color: "var(--text-2)" }}>چرا؟ </b>
              {p.whyFa}
            </p>
            <a className="pod-link" href={p.url} target="_blank" rel="noreferrer">
              رفتن به سایت رسمی
              <Icon name="external" width={12} height={12} />
            </a>
          </div>
        ))}
      </div>

      {list.length === 0 && (
        <div className="empty">
          <Icon name="search" width={26} height={26} />
          <p style={{ marginTop: 10 }}>پادکستی با این فیلتر پیدا نشد.</p>
        </div>
      )}

      <div className="explore-note">
        <Icon name="info" width={16} height={16} />
        <div>
          لینک‌ها به سایت رسمی هر پادکست می‌روند. بعضی از این منابع متن کامل قسمت‌ها را
          هم منتشر می‌کنند؛ اگر متن و فایل صوتی را دانلود کنی، می‌توانی آن را در بخش{" "}
          <a href="#/import" style={{ color: "var(--accent)", fontWeight: 700 }}>
            افزودن درس
          </a>{" "}
          وارد کنی تا زیرنویس واژه‌به‌واژه و آزمون داشته باشد.
        </div>
      </div>
    </div>
  );
}
