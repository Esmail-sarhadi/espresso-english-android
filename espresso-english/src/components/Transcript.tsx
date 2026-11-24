import { memo, useEffect, useMemo, useRef } from "react";
import type { Episode, Segment } from "../lib/types";
import { cleanWord } from "../lib/format";
import { Icon } from "./Icon";

interface Props {
  episode: Episode;
  activeId?: string;
  activeWord?: number;
  activeSegment?: Segment;
  showFa: boolean;
  blurFa: boolean;
  autoScroll: boolean;
  savedTerms: Set<string>;
  keyTerms: Set<string>;
  repeatId?: string;
  onWordClick: (word: string, segment: Segment, rect: DOMRect) => void;
  onSegmentClick: (segment: Segment) => void;
  onRepeat: (segment: Segment) => void;
  playing: boolean;
}

export const Transcript = memo(function Transcript({
  episode,
  activeId,
  activeWord,
  showFa,
  blurFa,
  autoScroll,
  savedTerms,
  keyTerms,
  repeatId,
  onWordClick,
  onSegmentClick,
  onRepeat,
  playing,
}: Props) {
  const wrapRef = useRef<HTMLDivElement>(null);
  const userScrollAt = useRef(0);

  useEffect(() => {
    const el = wrapRef.current;
    if (!el) return;
    const onWheel = () => (userScrollAt.current = Date.now());
    const onTouch = () => (userScrollAt.current = Date.now());
    el.addEventListener("wheel", onWheel, { passive: true });
    el.addEventListener("touchmove", onTouch, { passive: true });
    return () => {
      el.removeEventListener("wheel", onWheel);
      el.removeEventListener("touchmove", onTouch);
    };
  }, []);

  useEffect(() => {
    if (!autoScroll || !activeId) return;
    if (Date.now() - userScrollAt.current < 7000) return;
    const el = wrapRef.current?.querySelector<HTMLElement>(`[data-seg="${activeId}"]`);
    if (!el) return;
    const rect = el.getBoundingClientRect();
    const wrapRect = wrapRef.current!.getBoundingClientRect();
    const outside = rect.top < wrapRect.top + 80 || rect.bottom > wrapRect.bottom - 80;
    if (outside) el.scrollIntoView({ behavior: "smooth", block: "center" });
  }, [activeId, autoScroll]);

  const segments = useMemo(() => episode.segments, [episode]);

  return (
    <div className="transcript" ref={wrapRef}>
      {segments.map((seg) => {
        const isActive = seg.id === activeId;
        return (
          <div
            key={seg.id}
            data-seg={seg.id}
            className={`line${isActive ? " is-active" : ""}`}
            onClick={() => onSegmentClick(seg)}
          >
            <div className="line-gutter">
              <button
                className="line-play"
                onClick={(e) => {
                  e.stopPropagation();
                  onSegmentClick(seg);
                }}
                aria-label="پخش این جمله"
              >
                <Icon name={isActive && playing ? "pause" : "play"} width={12} height={12} />
              </button>
            </div>
            <div className="line-body">
              <p className="line-en en">
                {seg.words.map((w, i) => {
                  const key = cleanWord(w.w);
                  const isSaved = savedTerms.has(key);
                  const isKey = keyTerms.has(key);
                  const isWordActive = isActive && activeWord === i;
                  return (
                    <span key={i}>
                      <span
                        className={`word${isWordActive ? " is-word-active" : ""}${
                          isSaved ? " is-saved" : ""
                        }${isKey ? " is-key" : ""}`}
                        data-word={key}
                        onClick={(e) => {
                          e.stopPropagation();
                          onWordClick(w.w, seg, (e.target as HTMLElement).getBoundingClientRect());
                        }}
                      >
                        {w.w}
                      </span>{" "}
                    </span>
                  );
                })}
              </p>
              {showFa && seg.fa && (
                <p className={`line-fa${blurFa ? " is-blur" : ""}`}>{seg.fa}</p>
              )}
            </div>
            <button
              className={`line-loop${repeatId === seg.id ? " is-on" : ""}`}
              title="تکرار این جمله"
              onClick={(e) => {
                e.stopPropagation();
                onRepeat(seg);
              }}
            >
              <Icon name="repeat" width={13} height={13} />
            </button>
          </div>
        );
      })}
    </div>
  );
});
