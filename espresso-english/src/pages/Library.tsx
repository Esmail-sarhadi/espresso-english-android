import { useMemo, useState } from "react";
import { useData, useProgress } from "../lib/store";
import { EpisodeCard } from "./Home";
import { categoryLabels, fa } from "../lib/format";
import { Icon } from "../components/Icon";
import type { Category, Level } from "../lib/types";

const CATS: { key: Category | "all"; label: string }[] = [
  { key: "all", label: "همه" },
  { key: "grammar", label: "گرامر" },
  { key: "vocabulary", label: "واژگان" },
  { key: "phrases", label: "عبارت‌ها" },
  { key: "pronunciation", label: "تلفظ" },
  { key: "idioms", label: "اصطلاح‌ها" },
  { key: "conversation", label: "مکالمه" },
];

const LEVELS: { key: Level | "all"; label: string }[] = [
  { key: "all", label: "همه سطوح" },
  { key: "beginner", label: "مبتدی" },
  { key: "intermediate", label: "متوسط" },
  { key: "advanced", label: "پیشرفته" },
];

export function Library({ query }: { query: string }) {
  const { episodes } = useData();
  const progress = useProgress();
  const initialQ = useMemo(() => {
    const m = query.match(/(?:^|&)q=([^&]*)/);
    return m ? decodeURIComponent(m[1]) : "";
  }, [query]);

  const [cat, setCat] = useState<Category | "all">("all");
  const [level, setLevel] = useState<Level | "all">("all");
  const [q, setQ] = useState(initialQ);
  const [sort, setSort] = useState<"default" | "short" | "progress">("default");

  const list = useMemo(() => {
    let out = episodes.filter((e) => {
      if (cat !== "all" && e.category !== cat) return false;
      if (level !== "all" && e.level !== level) return false;
      if (q.trim()) {
        const needle = q.trim().toLowerCase();
        const hay = `${e.title} ${e.titleFa} ${e.descriptionFa} ${e.tags.join(" ")} ${e.vocab
          .map((v) => `${v.term} ${v.fa}`)
          .join(" ")}`.toLowerCase();
        if (!hay.includes(needle)) return false;
      }
      return true;
    });
    if (sort === "short") out = [...out].sort((a, b) => a.duration - b.duration);
    if (sort === "progress")
      out = [...out].sort(
        (a, b) => (progress[b.id]?.position ?? 0) - (progress[a.id]?.position ?? 0)
      );
    return out;
  }, [episodes, cat, level, q, sort, progress]);

  return (
    <div>
      <div className="page-head">
        <div>
          <h1 className="page-title">کتابخانه درس‌ها</h1>
          <p className="page-sub">
            {fa(episodes.length)} درس صوتی با زیرنویس واژه‌به‌واژه، معنی فارسی و آزمون اختصاصی
          </p>
        </div>
      </div>

      <div className="filters">
        {CATS.map((c) => (
          <button
            key={c.key}
            className="chip"
            data-active={cat === c.key}
            onClick={() => setCat(c.key)}
          >
            {c.label}
          </button>
        ))}
        <div className="filter-sep" />
        {LEVELS.map((l) => (
          <button
            key={l.key}
            className="chip"
            data-active={level === l.key}
            onClick={() => setLevel(l.key)}
          >
            {l.label}
          </button>
        ))}
        <div className="filter-sep" />
        <select
          className="chip"
          value={sort}
          onChange={(e) => setSort(e.target.value as typeof sort)}
          style={{ paddingInline: 12, appearance: "none" }}
          aria-label="مرتب‌سازی"
        >
          <option value="default">ترتیب پیشنهادی</option>
          <option value="short">کوتاه‌ترین</option>
          <option value="progress">بیشترین پیشرفت</option>
        </select>
        <span className="result-count">{fa(list.length)} درس</span>
      </div>

      {q && (
        <div className="search-active">
          <Icon name="search" width={13} height={13} />
          جست‌وجو برای «{q}»
          <button className="btn btn-ghost btn-sm" onClick={() => setQ("")}>
            <Icon name="x" width={12} height={12} />
            پاک کردن
          </button>
        </div>
      )}

      {list.length ? (
        <div className="grid-episodes">
          {list.map((ep, i) => (
            <EpisodeCard key={ep.id} episode={ep} index={i} />
          ))}
        </div>
      ) : (
        <div className="empty">
          <Icon name="search" width={26} height={26} />
          <p style={{ marginTop: 10 }}>درسی با این فیلترها پیدا نشد.</p>
          <button
            className="btn btn-outline btn-sm"
            style={{ marginTop: 14 }}
            onClick={() => {
              setCat("all");
              setLevel("all");
              setQ("");
            }}
          >
            پاک کردن فیلترها
          </button>
        </div>
      )}

      <div className="lib-note">
        <Icon name="info" width={15} height={15} />
        <div>
          فایل‌های صوتی خودت را هم می‌توانی اضافه کنی: در صفحه{" "}
          <a href="#/import" style={{ color: "var(--accent)", fontWeight: 700 }}>
            افزودن درس
          </a>{" "}
          یک mp3 به همراه متن (یا فایل srt) بده تا همین زیرنویس هوشمند و آزمون برایش ساخته شود.
        </div>
      </div>
    </div>
  );
}
