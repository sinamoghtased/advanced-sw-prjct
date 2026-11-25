"use client";

import { useRouter } from "next/navigation";
import { useEffect, useId, useState } from "react";
import {
  MODE_LABELS,
  ORDER_LABELS,
  PAGE_SIZES,
  RESULT_ORDERS,
  SEARCH_MODES,
  type SearchSettings,
  addSettingsToParams,
} from "@/services/engine/settings";

const DEBOUNCE_MS = 120;

function searchHref(query: string, index: string, settings: SearchSettings): string {
  const params = new URLSearchParams({ q: query });
  if (index) params.set("index", index);
  addSettingsToParams(params, settings);
  return `/?${params}`;
}

/**
 * The search box, with a dropdown of suggestions while the user types (FR12.0). Without
 * JavaScript it is a plain form that submits to the results page.
 */
export default function SearchBox({
  query,
  index,
  settings: initialSettings,
  large = false,
}: {
  query: string;
  index: string;
  settings: SearchSettings;
  large?: boolean;
}) {
  const router = useRouter();
  const listId = useId();
  const [value, setValue] = useState(query);
  const [typed, setTyped] = useState(false);
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(-1);
  const [settings, setSettings] = useState(initialSettings);
  const [symbols, setSymbols] = useState(initialSettings.symbols);

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
    router.push(searchHref(trimmed, index, { ...settings, symbols: symbols.replace(/\s+/gu, "") }));
  }

  /** Applies a changed setting: the search that is showing is run again with it. */
  function change(next: SearchSettings) {
    setSettings(next);
    if (query.trim() !== "") router.push(searchHref(query, index, next));
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

      <details className="mt-2 text-sm" open={settingsAreChanged(initialSettings)}>
        <summary className="w-fit cursor-pointer select-none px-2 py-1 text-zinc-600 dark:text-zinc-400">
          Search settings
        </summary>
        <div className="mt-2 grid gap-3 rounded-lg border border-black/[.12] p-3 sm:grid-cols-2 dark:border-white/[.2]">
          <label className="flex flex-col gap-1">
            <span className="font-medium">Search mode</span>
            <select
              value={settings.mode}
              onChange={(e) => change({ ...settings, mode: e.target.value as SearchSettings["mode"] })}
              className="h-9 rounded-md border border-black/[.15] bg-background px-2 dark:border-white/[.25]"
            >
              {SEARCH_MODES.map((m) => (
                <option key={m} value={m}>
                  {MODE_LABELS[m]}
                </option>
              ))}
            </select>
          </label>
          <label className="flex flex-col gap-1">
            <span className="font-medium">Order results by</span>
            <select
              value={settings.order}
              onChange={(e) => change({ ...settings, order: e.target.value as SearchSettings["order"] })}
              className="h-9 rounded-md border border-black/[.15] bg-background px-2 dark:border-white/[.25]"
            >
              {RESULT_ORDERS.map((o) => (
                <option key={o} value={o}>
                  {ORDER_LABELS[o]}
                </option>
              ))}
            </select>
          </label>
          <label className="flex flex-col gap-1">
            <span className="font-medium">Results per page</span>
            <select
              value={settings.size}
              onChange={(e) => change({ ...settings, size: Number(e.target.value) })}
              className="h-9 rounded-md border border-black/[.15] bg-background px-2 dark:border-white/[.25]"
            >
              {PAGE_SIZES.map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
            </select>
          </label>
          <label className="flex flex-col gap-1">
            <span className="font-medium">Symbols to ignore</span>
            <input
              type="text"
              value={symbols}
              onChange={(e) => setSymbols(e.target.value)}
              onBlur={() => {
                const cleaned = symbols.replace(/\s+/gu, "");
                if (cleaned !== settings.symbols) change({ ...settings, symbols: cleaned });
              }}
              onKeyDown={(e) => {
                if (e.key === "Enter") e.currentTarget.blur();
              }}
              placeholder="for example -&amp;&apos;"
              className="h-9 rounded-md border border-black/[.15] bg-transparent px-2 dark:border-white/[.25]"
            />
          </label>
          <label className="flex items-center gap-2 sm:col-span-2">
            <input
              type="checkbox"
              checked={settings.caseSensitive}
              onChange={(e) => change({ ...settings, caseSensitive: e.target.checked })}
              className="size-4 accent-current"
            />
            <span className="font-medium">Match case</span>
          </label>
          <p className="text-zinc-500 sm:col-span-2">
            With Match case on, &ldquo;Java&rdquo; does not find &ldquo;java&rdquo;. Ignored symbols are
            left out of the search words and of the pages when matching, so ignoring &ldquo;-&rdquo; lets
            &ldquo;selfdriving&rdquo; find &ldquo;self-driving&rdquo;.
          </p>
        </div>
      </details>

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

/** Whether any setting differs from its default, so the panel starts open. */
function settingsAreChanged(s: SearchSettings): boolean {
  const params = new URLSearchParams();
  addSettingsToParams(params, s);
  return params.size > 0;
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
