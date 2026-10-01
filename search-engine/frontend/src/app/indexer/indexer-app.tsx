"use client";

import Link from "next/link";
import { useEffect, useReducer, useRef, useState } from "react";
import {
  type Entry,
  type EngineEvent,
  type SearchResponse,
  type Summary,
  MAX_CHARACTERS,
  errorMessage,
  readEvents,
  splitLines,
  toEntry,
  validate,
  wordsOf,
} from "@/services/engine/client";

const PAGE_SIZE = 25;
const SLOW_MOTION_DELAY = 1500;

type LineShifts = { line: number; url: string | null; shifts: Entry[] };

type IndexState = {
  status: "idle" | "building" | "done" | "error";
  indexId: string | null;
  expectedLines: number;
  entries: Entry[];
  lines: LineShifts[];
  latest: Set<number>;
  summary: Summary | null;
  error: string | null;
};

type Action =
  | { type: "start"; expectedLines: number }
  | { type: "events"; events: EngineEvent[] }
  | { type: "fail"; message: string };

const initialState: IndexState = {
  status: "idle",
  indexId: null,
  expectedLines: 0,
  entries: [],
  lines: [],
  latest: new Set(),
  summary: null,
  error: null,
};

function reducer(state: IndexState, action: Action): IndexState {
  switch (action.type) {
    case "start":
      return { ...initialState, status: "building", expectedLines: action.expectedLines };
    case "fail":
      return { ...state, status: "error", error: action.message };
    case "events": {
      let next = state;
      let entries: Entry[] | null = null;
      for (const event of action.events) {
        if (event.type === "start") {
          next = { ...next, indexId: event.index };
        } else if (event.type === "update") {
          entries ??= next.entries.slice();
          // Inserting in ascending position order reproduces the engine's sorted index.
          for (const placed of event.inserted) entries.splice(placed.pos, 0, toEntry(placed));
          const shifts = event.generated;
          next = {
            ...next,
            lines: [...next.lines, { line: event.line, url: shifts[0]?.url ?? null, shifts }],
            latest: new Set(shifts.map((s) => s.id)),
          };
        } else if (event.type === "done") {
          const { linesIndexed, linesSkipped, shifts, elapsedMillis } = event;
          next = {
            ...next,
            status: "done",
            latest: new Set(),
            summary: { linesIndexed, linesSkipped, shifts, elapsedMillis },
          };
        } else if (event.type === "error") {
          next = { ...next, status: "error", error: event.message };
        }
      }
      return entries ? { ...next, entries } : next;
    }
  }
}

export default function IndexerApp() {
  const [text, setText] = useState("");
  const [inputError, setInputError] = useState<string | null>(null);
  const [slowMotion, setSlowMotion] = useState(false);
  const [state, dispatch] = useReducer(reducer, initialState);
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<{ query: string; lines: number; matches: Entry[] } | null>(null);
  const [searchError, setSearchError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [copyNotice, setCopyNotice] = useState("");
  const abortRef = useRef<AbortController | null>(null);

  const building = state.status === "building";
  const characterCount = Array.from(text).length;

  useEffect(() => () => abortRef.current?.abort(), []);

  // Search the finished index as the user types (FR9.0).
  useEffect(() => {
    const term = query.trim();
    if (term === "" || state.status !== "done" || !state.indexId) return;
    const controller = new AbortController();
    const timer = setTimeout(async () => {
      try {
        const params = new URLSearchParams({ index: state.indexId!, q: term, limit: "1000" });
        const response = await fetch(`/api/search?${params}`, { signal: controller.signal });
        if (!response.ok) {
          setSearchError(await errorMessage(response));
          return;
        }
        setSearchError(null);
        // Show the matching index entries, in index order.
        const found = (await response.json()) as SearchResponse;
        const matches = found.results
          .flatMap((r) => r.matches)
          .sort((a, b) => a.pos - b.pos)
          .map(toEntry);
        setResults({ query: found.query, lines: found.total, matches });
      } catch (e) {
        if ((e as Error).name !== "AbortError") setSearchError("The search failed. Try again.");
      }
    }, 150);
    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [query, state.status, state.indexId]);

  async function createIndex() {
    const problem = validate(text);
    setInputError(problem);
    if (problem) return;

    abortRef.current?.abort();
    setCopyNotice("");
    const controller = new AbortController();
    abortRef.current = controller;
    setQuery("");
    setResults(null);
    setSearchError(null);
    setPage(0);
    const expectedLines = splitLines(text).filter((line) => wordsOf(line).length > 0).length;
    dispatch({ type: "start", expectedLines });

    try {
      const url = slowMotion ? `/api/index?delay=${SLOW_MOTION_DELAY}` : "/api/index";
      const response = await fetch(url, { method: "POST", body: text, signal: controller.signal });
      if (!response.ok || !response.body) {
        const message = await errorMessage(response);
        setInputError(message);
        dispatch({ type: "fail", message });
        return;
      }
      for await (const events of readEvents(response.body)) {
        dispatch({ type: "events", events });
      }
    } catch (e) {
      if ((e as Error).name !== "AbortError") {
        dispatch({ type: "fail", message: "The connection to Quickdex was lost. Try again." });
      }
    }
  }

  function changeQuery(value: string) {
    setQuery(value);
    setPage(0);
    if (value.trim() === "") {
      setResults(null);
      setSearchError(null);
    }
  }

  async function copyEntries(label: string, entries: Entry[]) {
    const content = entries
      .map((entry) => [entry.keyword, entry.context, entry.line, entry.url ?? ""].join("\t"))
      .join("\n");
    try {
      await navigator.clipboard.writeText(content);
      setCopyNotice(`${label} copied.`);
    } catch {
      setCopyNotice("Clipboard access failed. Select the table text and copy it instead.");
    }
  }

  const searching = query.trim() !== "" && results !== null;
  const rows = searching ? results.matches : state.entries;
  const pageCount = Math.max(1, Math.ceil(rows.length / PAGE_SIZE));
  const currentPage = Math.min(page, pageCount - 1);
  const pageRows = rows.slice(currentPage * PAGE_SIZE, (currentPage + 1) * PAGE_SIZE);
  const indexedLines = state.lines.length;
  const shifts = state.lines.flatMap((l) => l.shifts);

  return (
    <div className="flex flex-col gap-10">
      {/* Step 1 and 2: enter text, then create the index. */}
      <section aria-labelledby="input-heading" className="flex flex-col gap-3">
        <h2 id="input-heading" className="text-lg font-semibold">
          1. Enter text
        </h2>
        <p className="text-sm text-zinc-600 dark:text-zinc-400">
          One entry per line. A web address at the start or end of a line becomes that
          line&apos;s link.
        </p>
        <textarea
          value={text}
          onChange={(e) => {
            setText(e.target.value);
            if (inputError) setInputError(null);
          }}
          rows={9}
          spellCheck={false}
          aria-label="Input text"
          aria-invalid={inputError !== null}
          aria-describedby="input-status"
          placeholder={"https://en.wikipedia.org/wiki/The_Descent_of_Man Descent of Man\nThe Ascent of Man"}
          className="w-full resize-y rounded-lg border border-black/[.12] bg-transparent p-3 font-mono text-sm leading-6 outline-none focus:border-foreground dark:border-white/[.2]"
        />
        <div id="input-status" className="flex flex-wrap items-start justify-between gap-2 text-sm">
          {inputError ? (
            <p role="alert" className="font-medium text-red-700 dark:text-red-400">
              {inputError}
            </p>
          ) : (
            <p className="text-zinc-500">
              {characterCount.toLocaleString("en-US")} / {MAX_CHARACTERS.toLocaleString("en-US")} characters
            </p>
          )}
        </div>
        <div className="flex flex-wrap items-center gap-4">
          <button
            type="button"
            onClick={() => void createIndex()}
            disabled={building}
            className="flex h-11 items-center justify-center rounded-full bg-foreground px-6 text-sm font-medium text-background transition-colors hover:bg-[#383838] disabled:cursor-not-allowed disabled:opacity-40 dark:hover:bg-[#ccc]"
          >
            {building ? "Creating index…" : "2. Create index"}
          </button>
          <label className="flex items-center gap-2 text-sm text-zinc-600 dark:text-zinc-400">
            <input
              type="checkbox"
              checked={slowMotion}
              onChange={(e) => setSlowMotion(e.target.checked)}
              disabled={building}
              className="size-4 accent-current"
            />
            Slow motion: watch the index grow line by line
          </label>
        </div>
      </section>

      {state.status !== "idle" && (
        <section aria-labelledby="results-heading" className="flex flex-col gap-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h2 id="results-heading" className="text-lg font-semibold">
              3. View results
            </h2>
            <Status state={state} indexedLines={indexedLines} />
          </div>
          {state.status === "done" && state.indexId && (
            <Link
              href={`/?index=${state.indexId}`}
              className="flex w-fit items-center rounded-full bg-foreground px-5 py-2 text-sm font-medium text-background transition-colors hover:bg-[#383838] dark:hover:bg-[#ccc]"
            >
              Search these pages in the search engine →
            </Link>
          )}
          {building && state.expectedLines > 0 && (
            <div
              className="h-1.5 w-full overflow-hidden rounded-full bg-black/[.06] dark:bg-white/[.1]"
              aria-hidden
            >
              <div
                className="h-full rounded-full bg-foreground transition-[width] duration-200"
                style={{ width: `${Math.min(100, (indexedLines / state.expectedLines) * 100)}%` }}
              />
            </div>
          )}

          <div className="flex flex-col gap-6">
            {/* Circular shifts, in the order they were generated. */}
            <div className="flex min-w-0 flex-col gap-3">
              <div className="flex items-center justify-between gap-3">
                <h3 className="text-sm font-medium uppercase tracking-widest text-zinc-500">
                  Circular shifts
                </h3>
                <button
                  type="button"
                  onClick={() => void copyEntries("Circular shifts", shifts)}
                  disabled={shifts.length === 0}
                  className="rounded-full border border-black/[.12] px-3 py-1 text-sm disabled:opacity-40 dark:border-white/[.2]"
                >
                  Copy shifts
                </button>
              </div>
              <div className="max-h-[36rem] overflow-x-auto overflow-y-auto rounded-lg border-2 border-amber-500 dark:border-amber-400">
                <table className="w-full text-left text-sm">
                  <caption className="sr-only">
                    Circularly shifted lines, in the order they were generated: keyword, context,
                    source line, and link
                  </caption>
                  <thead className="border-b border-black/[.08] text-xs uppercase tracking-wider text-amber-700 dark:border-white/[.145] dark:text-amber-400">
                    <tr>
                      <th scope="col" className="px-3 py-2 font-medium">Keyword</th>
                      <th scope="col" className="px-3 py-2 font-medium">Context</th>
                      <th scope="col" className="px-3 py-2 text-right font-medium">Line</th>
                      <th scope="col" className="px-3 py-2 font-medium">Link</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-black/[.06] dark:divide-white/[.1]">
                    {shifts.length === 0 ? (
                      <tr>
                        <td colSpan={4} className="px-3 py-6 text-center text-amber-700/70 dark:text-amber-400/70">
                          Waiting for the first line…
                        </td>
                      </tr>
                    ) : (
                      shifts.map((s) => (
                        <tr
                          key={s.id}
                          className={state.latest.has(s.id) ? "bg-amber-100 dark:bg-amber-400/15" : ""}
                        >
                          <td className="px-3 py-2 align-top font-semibold text-amber-900 dark:text-amber-200">{s.keyword}</td>
                          <td className="px-3 py-2 align-top text-amber-800/80 dark:text-amber-300/80">{s.context}</td>
                          <td className="px-3 py-2 text-right align-top tabular-nums text-amber-700 dark:text-amber-400">{s.line}</td>
                          <td className="max-w-48 px-3 py-2 align-top">
                            <EntryLink url={s.url} />
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            {/* The alphabetized index, with search. */}
            <div className="flex min-w-0 flex-col gap-3">
              <div className="flex items-center justify-between gap-3">
                <h3 className="text-sm font-medium uppercase tracking-widest text-zinc-500">
                  Alphabetized index
                </h3>
                <button
                  type="button"
                  onClick={() => void copyEntries("Alphabetized index", rows)}
                  disabled={rows.length === 0}
                  className="rounded-full border border-black/[.12] px-3 py-1 text-sm disabled:opacity-40 dark:border-white/[.2]"
                >
                  Copy index
                </button>
              </div>
              <p className="min-h-5 text-sm text-zinc-500" aria-live="polite">{copyNotice}</p>

              <div className="flex flex-col gap-1">
                <input
                  type="search"
                  value={query}
                  onChange={(e) => changeQuery(e.target.value)}
                  disabled={state.status !== "done"}
                  placeholder={
                    state.status === "done"
                      ? "Filter by keyword, e.g. “man”"
                      : "Filtering is available once the index is finished"
                  }
                  aria-label="Filter the index by keyword"
                  className="h-10 w-full rounded-lg border border-black/[.12] bg-transparent px-3 text-sm outline-none focus:border-foreground disabled:opacity-50 dark:border-white/[.2]"
                />
                <p className="min-h-5 text-sm text-zinc-500" aria-live="polite">
                  {searchError ? (
                    <span className="text-red-700 dark:text-red-400">{searchError}</span>
                  ) : searching ? (
                    <>
                      {results.matches.length.toLocaleString("en-US")}{" "}
                      {results.matches.length === 1 ? "entry" : "entries"} in{" "}
                      {results.lines.toLocaleString("en-US")} {results.lines === 1 ? "line" : "lines"} match “
                      {results.query}”
                    </>
                  ) : (
                    `${state.entries.length.toLocaleString("en-US")} entries`
                  )}
                </p>
              </div>

              <div className="overflow-x-auto rounded-lg border-2 border-green-600 dark:border-green-400">
                <table className="w-full text-left text-sm">
                  <caption className="sr-only">
                    Alphabetized KWIC index: keyword, context, source line, and link
                  </caption>
                  <thead className="border-b border-black/[.08] text-xs uppercase tracking-wider text-green-700 dark:border-white/[.145] dark:text-green-400">
                    <tr>
                      <th scope="col" className="px-3 py-2 font-medium">Keyword</th>
                      <th scope="col" className="px-3 py-2 font-medium">Context</th>
                      <th scope="col" className="px-3 py-2 text-right font-medium">Line</th>
                      <th scope="col" className="px-3 py-2 font-medium">Link</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-black/[.06] dark:divide-white/[.1]">
                    {pageRows.length === 0 ? (
                      <tr>
                        <td colSpan={4} className="px-3 py-6 text-center text-green-700/70 dark:text-green-400/70">
                          {searching ? "No lines contain every word." : "No entries yet."}
                        </td>
                      </tr>
                    ) : (
                      pageRows.map((e) => (
                        <tr
                          key={e.id}
                          className={!searching && state.latest.has(e.id) ? "bg-amber-100 dark:bg-amber-400/15" : ""}
                        >
                          <td className="px-3 py-2 align-top font-semibold text-green-900 dark:text-green-200">{e.keyword}</td>
                          <td className="px-3 py-2 align-top text-green-800/80 dark:text-green-300/80">{e.context}</td>
                          <td className="px-3 py-2 text-right align-top tabular-nums text-green-700 dark:text-green-400">{e.line}</td>
                          <td className="max-w-48 px-3 py-2 align-top">
                            <EntryLink url={e.url} />
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>

              {pageCount > 1 && (
                <nav aria-label="Index pages" className="flex items-center justify-between gap-2 text-sm">
                  <button
                    type="button"
                    onClick={() => setPage(currentPage - 1)}
                    disabled={currentPage === 0}
                    className="rounded-full border border-black/[.12] px-3 py-1 disabled:opacity-40 dark:border-white/[.2]"
                  >
                    Previous
                  </button>
                  <span className="text-zinc-500 tabular-nums">
                    Page {currentPage + 1} of {pageCount.toLocaleString("en-US")}
                  </span>
                  <button
                    type="button"
                    onClick={() => setPage(currentPage + 1)}
                    disabled={currentPage >= pageCount - 1}
                    className="rounded-full border border-black/[.12] px-3 py-1 disabled:opacity-40 dark:border-white/[.2]"
                  >
                    Next
                  </button>
                </nav>
              )}
            </div>
          </div>
        </section>
      )}
    </div>
  );
}

function Status({ state, indexedLines }: { state: IndexState; indexedLines: number }) {
  let message: string;
  if (state.status === "building") {
    message = `Indexing… ${indexedLines.toLocaleString("en-US")} of ${state.expectedLines.toLocaleString("en-US")} lines`;
  } else if (state.status === "done" && state.summary) {
    const s = state.summary;
    message = `Done: ${s.linesIndexed.toLocaleString("en-US")} lines, ${s.shifts.toLocaleString("en-US")} entries in ${(s.elapsedMillis / 1000).toFixed(2)} s`;
  } else if (state.status === "error") {
    message = state.error ?? "Something went wrong. Try again.";
  } else {
    message = "";
  }
  return (
    <p
      role="status"
      className={`text-sm ${state.status === "error" ? "font-medium text-red-700 dark:text-red-400" : "text-zinc-500"}`}
    >
      {message}
    </p>
  );
}

function EntryLink({ url }: { url: string | null }) {
  if (!url || !/^https?:\/\//i.test(url)) {
    return <span className="text-zinc-400">—</span>;
  }
  let label = url;
  try {
    const parsed = new URL(url);
    label = parsed.hostname + (parsed.pathname === "/" ? "" : parsed.pathname);
  } catch {
    // Show the raw address.
  }
  return (
    <a
      href={url}
      target="_blank"
      rel="noopener noreferrer"
      title={url}
      className="block truncate text-blue-700 underline-offset-2 hover:underline dark:text-blue-400"
    >
      {label}
    </a>
  );
}
