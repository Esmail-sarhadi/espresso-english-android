import { useEffect, useLayoutEffect, useRef, useState } from "react";
import type { Episode, Segment } from "../lib/types";
import { lookup, type LookupResult } from "../lib/lookup";
import { addWord } from "../lib/storage";
import { useData, useVocab, usePlayer } from "../lib/store";
import { displayWord } from "../lib/format";
import { Icon } from "./Icon";

interface Props {
  word: string;
  episode: Episode;
  segment: Segment;
  anchor: DOMRect;
  onClose: () => void;
}

export function speak(text: string, rate = 0.92) {
  try {
    const synth = window.speechSynthesis;
    synth.cancel();
    const utter = new SpeechSynthesisUtterance(text);
    utter.lang = "en-US";
    utter.rate = rate;
    const voices = synth.getVoices().filter((v) => /en[-_]/i.test(v.lang));
    const preferred =
      voices.find((v) => /Google US English|Samantha|Aria|Jenny|Zira/i.test(v.name)) ?? voices[0];
    if (preferred) utter.voice = preferred;
    synth.speak(utter);
  } catch {
    /* ignore */
  }
}

export function WordPanel({ word, episode, segment, anchor, onClose }: Props) {
  const { glossary } = useData();
  const { vocab, setVocab } = useVocab();
  const { playRange } = usePlayer();
  const [data, setData] = useState<LookupResult | null>(null);
  const [pos, setPos] = useState({ top: 0, left: 0 });
  const ref = useRef<HTMLDivElement>(null);
  const term = displayWord(word);

  useEffect(() => {
    let alive = true;
    setData(null);
    lookup(word, episode, glossary).then((res) => {
      if (alive) setData(res);
    });
    return () => {
      alive = false;
    };
  }, [word, episode, glossary]);

  useLayoutEffect(() => {
    if (!ref.current) return;
    const panel = ref.current.getBoundingClientRect();
    const margin = 14;
    let top = anchor.bottom + 10;
    if (top + panel.height > window.innerHeight - margin) {
      top = Math.max(margin, anchor.top - panel.height - 10);
    }
    let left = anchor.left + anchor.width / 2 - panel.width / 2;
    left = Math.max(margin, Math.min(left, window.innerWidth - panel.width - margin));
    setPos({ top, left });
  }, [anchor, data]);

  useEffect(() => {
    const onDown = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) onClose();
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    document.addEventListener("mousedown", onDown);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDown);
      document.removeEventListener("keydown", onKey);
    };
  }, [onClose]);

  const saved = vocab.some((v) => v.term.toLowerCase() === (data?.term ?? term).toLowerCase());

  const save = () => {
    if (!data) return;
    setVocab(
      addWord(vocab, {
        term: data.term || term,
        fa: data.fa ?? "—",
        ipa: data.ipa,
        pos: data.pos,
        en: data.en,
        example: data.example,
        episodeId: episode.id,
        episodeTitle: episode.titleFa,
        segmentId: segment.id,
      })
    );
  };

  const fa = data?.fa;

  return (
    <div
      ref={ref}
      className="word-panel"
      style={{ top: pos.top, left: pos.left }}
      role="dialog"
      aria-label={`معنی ${term}`}
    >
      <div className="wp-head">
        <div>
          <div className="wp-word en">{displayWord(data?.term ?? word)}</div>
          <div className="wp-sub">
            {data?.ipa && <span className="wp-ipa num">{data.ipa}</span>}
            {data?.pos && <span className="wp-pos">{data.pos}</span>}
          </div>
        </div>
        <div style={{ display: "flex", gap: 4 }}>
          <button
            className="btn btn-icon btn-sm btn-ghost"
            title="تلفظ"
            onClick={() => speak(displayWord(data?.term ?? word))}
          >
            <Icon name="volume" width={15} height={15} />
          </button>
          <button className="btn btn-icon btn-sm btn-ghost" title="بستن" onClick={onClose}>
            <Icon name="x" width={15} height={15} />
          </button>
        </div>
      </div>

      {!data && (
        <div style={{ display: "grid", gap: 8, padding: "6px 0" }}>
          <div className="skeleton" style={{ height: 26, width: "70%" }} />
          <div className="skeleton" style={{ height: 14, width: "90%" }} />
        </div>
      )}

      {data && (
        <>
          <div className="wp-fa">{fa || "معنی‌ای پیدا نشد"}</div>
          {data.en && <div className="wp-en en">{data.en}</div>}
          {data.example && (
            <div className="wp-example en">
              <Icon name="text" width={12} height={12} />
              <span>{data.example}</span>
            </div>
          )}
          <div className="wp-context">
            <div className="wp-context-label">در این درس</div>
            <div className="en wp-context-text">{segment.text}</div>
            {segment.fa && <div className="wp-context-fa">{segment.fa}</div>}
          </div>
          <div className="wp-actions">
            <button
              className="btn btn-sm"
              onClick={() => {
                playRange(segment.start, segment.end);
                onClose();
              }}
            >
              <Icon name="play" width={12} height={12} />
              پخش جمله
            </button>
            <button
              className="btn btn-sm btn-primary"
              onClick={save}
              disabled={saved}
              style={{ marginInlineStart: "auto" }}
            >
              <Icon name={saved ? "check" : "bookmark"} width={13} height={13} />
              {saved ? "ذخیره شد" : "افزودن به واژه‌نامه"}
            </button>
          </div>
          {data.source === "online" && !fa && (
            <div className="wp-note">
              ترجمه فارسی از سرویس آنلاین دریافت نشد؛ می‌توانید معنی را خودتان در واژه‌نامه ثبت کنید.
            </div>
          )}
        </>
      )}
    </div>
  );
}
