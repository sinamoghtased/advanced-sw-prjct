"use client";

import { useRouter } from "next/navigation";
import { useEffect, useId, useState } from "react";

const DEBOUNCE_MS = 120;

function searchHref(query: string, index: string): string {
  const params = new URLSearchParams({ q: query });
  if (index) params.set("index", index);
  return `/?${params}`;
}

/**
 * The search box, with a dropdown of suggestions while the user types (FR12.0). Without
 * JavaScript it is a plain form that submits to the results page.
 */
export default function SearchBox({
  query,
  index,
  large = false,
}: {
  query: string;
  index: string;
  large?: boolean;
}) {
  const router = useRouter();
  const listId = useId();
  const [value, setValue] = useState(query);
  const [typed, setTyped] = useState(false);
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(-1);

  // Fetch suggestions for what the user has typed, after a short pause.
  useEffect(() => {
    if (!typed || value.trim() === "") return;
    const controller = new AbortController();
    const timer = setTimeout(async () => {
      try {
        const params = new URLSearchParams({ q: value });
        if (index) params.set("index", index);
        const response = await fetch(`/api/suggest?${params}`, { signal: controller.signal });
        if (!response.ok) return;
        const data = (await response.json()) as { suggestions: string[] };
        setSuggestions(data.suggestions);
        setActive(-1);
        setOpen(true);
      } catch {
        // Suggestions are optional; ignore failures.
      }
    }, DEBOUNCE_MS);
    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [value, typed, index]);

  function go(search: string) {
    const trimmed = search.trim();
    if (trimmed === "") return;
    setValue(trimmed);
    setOpen(false);
    setTyped(false);
    router.push(searchHref(trimmed, index));
  }

  function onKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    const showing = open && suggestions.length > 0;
    if (e.key === "ArrowDown" && showing) {
      e.preventDefault();
      setActive((a) => (a + 1) % suggestions.length);
    } else if (e.key === "ArrowUp" && showing) {
      e.preventDefault();
      setActive((a) => (a <= 0 ? suggestions.length - 1 : a - 1));
    } else if (e.key === "Enter" && showing && active >= 0) {
      e.preventDefault();
      go(suggestions[active]);
    } else if (e.key === "Escape") {
      setOpen(false);
    }
  }

  const showList = open && suggestions.length > 0 && value.trim() !== "";
  const optionId = (i: number) => `${listId}-option-${i}`;

  return (
    <div className={`relative w-full max-w-2xl ${large ? "" : "sm:flex-1"}`}>
      <form
        action="/"
        method="get"
        role="search"
        onSubmit={(e) => {
          e.preventDefault();
          go(value);
        }}
        className={`flex w-full items-center gap-2 border border-black/[.15] bg-background pl-4 pr-1.5 shadow-sm focus-within:border-foreground dark:border-white/[.25] ${
          large ? "h-14" : "h-12"
        } ${showList ? "rounded-t-3xl rounded-b-none border-b-transparent" : "rounded-full"}`}
      >
        <svg aria-hidden viewBox="0 0 24 24" className="size-5 shrink-0 text-zinc-500" fill="none" stroke="currentColor" strokeWidth="2">
          <circle cx="11" cy="11" r="7" />
          <path d="m20 20-3.5-3.5" strokeLinecap="round" />
        </svg>
        <input
          type="search"
          name="q"
          value={value}
          onChange={(e) => {
            setValue(e.target.value);
            setTyped(true);
            if (e.target.value.trim() === "") {
              setSuggestions([]);
              setOpen(false);
            }
          }}
          onKeyDown={onKeyDown}
          onFocus={() => suggestions.length > 0 && setOpen(true)}
          onBlur={() => setOpen(false)}
          required
          autoFocus={large}
          autoComplete="off"
          role="combobox"
          aria-label="Search"
          aria-autocomplete="list"
          aria-expanded={showList}
          aria-controls={listId}
          aria-activedescendant={showList && active >= 0 ? optionId(active) : undefined}
          placeholder="Search pages by keyword"
          className="h-full min-w-0 flex-1 bg-transparent text-base outline-none"
        />
        {index && <input type="hidden" name="index" value={index} />}
        <button
          type="submit"
          className="h-9 shrink-0 rounded-full bg-foreground px-4 text-sm font-medium text-background transition-colors hover:bg-[#383838] dark:hover:bg-[#ccc]"
        >
          Search
        </button>
      </form>

      <ul
        id={listId}
        role="listbox"
        aria-label="Suggestions"
        hidden={!showList}
        className="absolute inset-x-0 top-full z-20 overflow-hidden rounded-b-3xl border border-t-0 border-foreground bg-background pb-2 shadow-lg"
      >
        <li role="presentation" className="mx-4 mb-1 border-t border-black/[.08] dark:border-white/[.145]" />
        {suggestions.map((s, i) => (
          <li
            key={s}
            id={optionId(i)}
            role="option"
            aria-selected={i === active}
            onMouseDown={(e) => e.preventDefault()}
            onMouseEnter={() => setActive(i)}
            onClick={() => go(s)}
            className={`flex cursor-pointer items-center gap-3 px-4 py-1.5 text-left ${
              i === active ? "bg-black/[.05] dark:bg-white/[.08]" : ""
            }`}
          >
            <svg aria-hidden viewBox="0 0 24 24" className="size-4 shrink-0 text-zinc-400" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="11" cy="11" r="7" />
              <path d="m20 20-3.5-3.5" strokeLinecap="round" />
            </svg>
            <Suggestion text={s} typed={value} />
          </li>
        ))}
      </ul>
    </div>
  );
}

/** Shows what the user typed in normal weight and the completion in bold, like most search engines. */
function Suggestion({ text, typed }: { text: string; typed: string }) {
  const prefix = typed.trimStart().toLowerCase();
  if (prefix && text.startsWith(prefix)) {
    return (
      <span className="truncate">
        {text.slice(0, prefix.length)}
        <span className="font-semibold">{text.slice(prefix.length)}</span>
      </span>
    );
  }
  return <span className="truncate font-semibold">{text}</span>;
}
