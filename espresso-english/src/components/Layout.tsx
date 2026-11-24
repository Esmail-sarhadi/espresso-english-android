import { useEffect, useMemo, useState } from "react";
import { navigate, type Route } from "../App";
import { Icon, type IconName } from "./Icon";
import { useData, usePlayer, useProgress, useSettings, useVocab } from "../lib/store";
import { categoryColors, fa, fmtTime, todayKey } from "../lib/format";

const NAV: { name: string; label: string; icon: IconName; route: string }[] = [
  { name: "home", label: "خانه", icon: "home", route: "" },
  { name: "library", label: "کتابخانه", icon: "library", route: "library" },
  { name: "notebook", label: "واژه‌نامه من", icon: "notebook", route: "notebook" },
  { name: "review", label: "آزمون و مرور", icon: "quiz", route: "review" },
  { name: "explore", label: "پادکست‌ها", icon: "radio", route: "explore" },
];

export function Sidebar({ route }: { route: Route }) {
  const { settings, set } = useSettings();
  const { vocab } = useVocab();
  const progress = useProgress();

  const streak = useMemo(() => {
    const days: { key: string; label: string; on: boolean }[] = [];
    const labels = ["ی", "د", "س", "چ", "پ", "ج", "ش"];
    for (let i = 6; i >= 0; i--) {
      const d = new Date();
      d.setDate(d.getDate() - i);
      const key = todayKey(d);
      const on = Object.values(progress).some(
        (p) => todayKey(new Date(p.lastAt)) === key && p.listenSeconds > 20
      );
      days.push({ key, label: labels[d.getDay()], on });
    }
    return days;
  }, [progress]);

  return (
    <aside className="sidebar">
      <div className="brand">
        <div className="brand-mark">
          <Icon name="coffee" width={22} height={22} />
        </div>
        <div>
          <div className="brand-name">اسپرسو انگلیش</div>
          <div className="brand-sub">Learning Studio</div>
        </div>
      </div>

      <nav className="nav">
        <div className="nav-label">فضای یادگیری</div>
        {NAV.map((item) => (
          <a
            key={item.name}
            href={`#/${item.route}`}
            className="nav-item"
            data-active={route.name === item.name}
          >
            <Icon name={item.icon} />
            <span>{item.label}</span>
            {item.name === "notebook" && vocab.length > 0 && (
              <span className="nav-count">{fa(vocab.length)}</span>
            )}
          </a>
        ))}
        <div className="nav-label">ابزارها</div>
        <a href="#/import" className="nav-item" data-active={route.name === "import"}>
          <Icon name="upload" />
          <span>افزودن درس از فایل</span>
        </a>
      </nav>

      <div className="sidebar-foot">
        <MiniPlayer />
        <div className="streak-card">
          <div className="streak-top">
            <span>زنجیره این هفته</span>
            <span style={{ color: "var(--accent)", display: "flex", gap: 4, alignItems: "center" }}>
              <Icon name="flame" width={13} height={13} />
              {fa(streak.filter((d) => d.on).length)} روز
            </span>
          </div>
          <div className="streak-days">
            {streak.map((d) => (
              <div key={d.key} className="streak-day" data-on={d.on} title={d.key}>
                {d.label}
              </div>
            ))}
          </div>
        </div>
        <button
          className="theme-toggle"
          onClick={() => set("theme", settings.theme === "dark" ? "light" : "dark")}
        >
          <span style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <Icon name={settings.theme === "dark" ? "moon" : "sun"} width={16} height={16} />
            {settings.theme === "dark" ? "حالت شب" : "حالت روز"}
          </span>
          <span
            style={{
              width: 34,
              height: 20,
              borderRadius: 99,
              background: "var(--surface-3)",
              position: "relative",
            }}
          >
            <span
              style={{
                position: "absolute",
                top: 3,
                insetInlineStart: settings.theme === "dark" ? 3 : 17,
                width: 14,
                height: 14,
                borderRadius: "50%",
                background: "var(--accent)",
                transition: "all .2s",
              }}
            />
          </span>
        </button>
      </div>
    </aside>
  );
}

function MiniPlayer() {
  const { episode, playing, time, duration, toggle, seek } = usePlayer();
  if (!episode) return null;
  const pct = duration ? (time / duration) * 100 : 0;
  return (
    <div
      className="mini-player"
      style={{
        border: "1px solid var(--line)",
        borderRadius: 14,
        background: "var(--surface)",
        padding: 10,
        cursor: "pointer",
        position: "relative",
        overflow: "hidden",
      }}
      onClick={() => navigate(`lesson/${episode.id}`)}
    >
      <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
        <div
          style={{
            width: 36,
            height: 36,
            borderRadius: 10,
            background: categoryColors[episode.category],
            display: "grid",
            placeItems: "center",
            color: "#1b1208",
            flex: "none",
          }}
        >
          <Icon name="headphones" width={17} height={17} />
        </div>
        <div style={{ minWidth: 0, flex: 1 }}>
          <div
            className="en"
            style={{
              fontSize: 11.5,
              fontWeight: 700,
              whiteSpace: "nowrap",
              overflow: "hidden",
              textOverflow: "ellipsis",
            }}
          >
            {episode.title}
          </div>
          <div className="num" style={{ fontSize: 10.5, color: "var(--muted)" }}>
            {fmtTime(time)} / {fmtTime(duration)}
          </div>
        </div>
        <button
          className="btn btn-icon btn-sm"
          style={{ background: "var(--accent)", color: "var(--accent-ink)", border: 0 }}
          onClick={(e) => {
            e.stopPropagation();
            toggle();
          }}
          aria-label={playing ? "توقف" : "پخش"}
        >
          <Icon name={playing ? "pause" : "play"} width={13} height={13} />
        </button>
      </div>
      <div
        style={{ height: 3, background: "var(--surface-3)", borderRadius: 9, marginTop: 9 }}
        onClick={(e) => {
          e.stopPropagation();
          const rect = e.currentTarget.getBoundingClientRect();
          const ratio = (e.clientX - rect.left) / rect.width;
          seek(ratio * duration);
        }}
      >
        <div
          style={{
            height: "100%",
            width: `${pct}%`,
            background: "var(--accent)",
            borderRadius: 9,
            transition: "width .2s linear",
          }}
        />
      </div>
    </div>
  );
}

export function Topbar({ route }: { route: Route }) {
  const { episodes } = useData();
  const [q, setQ] = useState("");

  useEffect(() => {
    setQ("");
  }, [route.name]);

  const titles: Record<string, string> = {
    home: "خانه",
    library: "کتابخانه درس‌ها",
    lesson: "کلاس درس",
    notebook: "واژه‌نامه من",
    review: "آزمون و مرور",
    explore: "پادکست‌های دیگر",
    import: "افزودن درس",
  };

  const onSearch = (e: React.FormEvent) => {
    e.preventDefault();
    navigate(`library?q=${encodeURIComponent(q)}`);
  };

  return (
    <header className="topbar">
      <div className="topbar-title">{titles[route.name] ?? "اسپرسو انگلیش"}</div>
      <div className="topbar-crumb">
        <Icon name="chevron" width={12} height={12} style={{ transform: "rotate(180deg)" }} />
        <span>{fa(episodes.length)} درس فعال</span>
      </div>
      <div className="topbar-actions">
        <form className="search" onSubmit={onSearch}>
          <Icon name="search" />
          <input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="جست‌وجوی درس یا واژه…"
            aria-label="جست‌وجو"
          />
        </form>
        <button className="btn btn-primary btn-sm" onClick={() => navigate("review")}>
          <Icon name="sparkles" width={15} height={15} />
          مرور امروز
        </button>
      </div>
    </header>
  );
}
