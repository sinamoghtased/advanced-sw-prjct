import type { Metadata } from "next";
import Link from "next/link";
import SearchBox from "@/components/search-box";
import { getFromEngine } from "@/services/engine/backend";
import { type IngestResult, addWebPages, webSourceNames } from "@/services/web-search";
import {
  type SearchResponse,
  type SearchResult,
  searchWords,
} from "@/services/engine/client";

const PAGE_SIZE = 10;
const SHOWN_MATCHES = 2;
const SUGGESTIONS = [
  "software architecture",
  "sort",
  "search engine",
  "design pattern",
  "information hiding",
  "man",
];

type SearchParams = Promise<{ [key: string]: string | string[] | undefined }>;

function first(value: string | string[] | undefined): string {
  return (Array.isArray(value) ? value[0] : value) ?? "";
}

function href(query: string, index: string, page = 1): string {
  const params = new URLSearchParams();
  if (query) params.set("q", query);
  if (index) params.set("index", index);
  if (page > 1) params.set("page", String(page));
  const search = params.toString();
  return search ? `/?${search}` : "/";
}

export async function generateMetadata({
  searchParams,
}: {
  searchParams: SearchParams;
}): Promise<Metadata> {
  const query = first((await searchParams).q).trim();
  return { title: query ? `${query} · Quickdex Search` : "Quickdex Search" };
}

export default async function SearchPage({ searchParams }: { searchParams: SearchParams }) {
  const params = await searchParams;
  const query = first(params.q).trim();
  const index = first(params.index);
  const page = Math.max(1, Number.parseInt(first(params.page), 10) || 1);

  if (query === "") {
    return <Home index={index} />;
  }

  // Optional: fetch matching pages from live online sources into the web index (off by
  // default in Phase I, which answers only from the backend's own corpus.txt index).
  const live = index ? null : await addWebPages(query);
  const reply = await getFromEngine<SearchResponse>("/search", {
    q: query,
    index,
    offset: String((page - 1) * PAGE_SIZE),
    limit: String(PAGE_SIZE),
  });

  return (
    <main className="mx-auto flex w-full max-w-5xl flex-1 flex-col gap-6 px-4 py-6 sm:px-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:gap-6">
        <Link href={href("", index)} className="text-2xl font-semibold tracking-tight">
          <Wordmark />
        </Link>
        <SearchBox key={query} query={query} index={index} />
      </div>
      {index && <OwnIndexNotice query={query} />}

      {!reply.ok ? (
        <Notice>
          <p className="font-medium">{reply.error}</p>
          {reply.status === 404 && index && (
            <Link href={href(query, "")} className="mt-2 inline-block underline underline-offset-2">
              Search the web index instead
            </Link>
          )}
        </Notice>
      ) : (
        <Results response={reply.data} query={query} index={index} page={page} live={live} />
      )}
    </main>
  );
}

async function Home({ index }: { index: string }) {
  const stats = await getFromEngine<{ pages: number; entries: number }>("/stats", { index });
  return (
    <main className="flex flex-1 flex-col items-center justify-center gap-7 px-4 py-16">
      <div className="flex flex-col items-center gap-3 text-center">
        <h1 className="text-5xl font-semibold tracking-tight sm:text-6xl">
          <Wordmark />
        </h1>
        <p className="text-zinc-600 dark:text-zinc-400">
          A search engine built on a Key Word In Context index.
        </p>
      </div>
      <SearchBox query="" index={index} large />
      {stats.ok ? (
        <p className="text-sm text-zinc-500">
          Searching {stats.data.pages.toLocaleString("en-US")}{" "}
          {index ? "of your pages" : "pages"} · {stats.data.entries.toLocaleString("en-US")} index
          entries
          {!index && webSourceNames().length > 0 && ` · plus live pages from ${listNames(webSourceNames())}`}
        </p>
      ) : (
        <Notice>
          <p className="font-medium">{stats.error}</p>
        </Notice>
      )}
      {index ? (
        <OwnIndexNotice query="" />
      ) : (
        <div className="flex max-w-2xl flex-wrap justify-center gap-2 text-sm">
          <span className="py-1.5 text-zinc-500">Try:</span>
          {SUGGESTIONS.map((s) => (
            <Link
              key={s}
              href={href(s, "")}
              className="rounded-full border border-black/[.12] px-3 py-1.5 transition-colors hover:bg-black/[.04] dark:border-white/[.2] dark:hover:bg-white/[.06]"
            >
              {s}
            </Link>
          ))}
        </div>
      )}
    </main>
  );
}

function Results({
  response,
  query,
  index,
  page,
  live,
}: {
  response: SearchResponse;
  query: string;
  index: string;
  page: number;
  live: IngestResult;
}) {
  const words = searchWords(query);
  const pageCount = Math.ceil(response.total / PAGE_SIZE);
  const millis = response.elapsedMicros / 1000;
  const time = millis < 1000 ? `${millis.toFixed(1)} ms` : `${(millis / 1000).toFixed(2)} s`;

  return (
    <>
      <p className="text-sm text-zinc-500">
        {response.total.toLocaleString("en-US")} {response.total === 1 ? "result" : "results"} ({time})
        · searched {response.pages.toLocaleString("en-US")} pages and{" "}
        {response.entries.toLocaleString("en-US")} index entries
        {live && live.added > 0 && ` · ${live.added} new from ${listNames(live.used)}`}
      </p>
      {live && live.failed.length > 0 && (
        <p className="text-sm text-amber-700 dark:text-amber-300">
          {listNames(live.failed)} did not respond; showing pages from the other sources and the
          index.
        </p>
      )}
      {response.results.length > 0 && response.results[0].missing.length > 0 && (
        <p className="text-sm text-zinc-600 dark:text-zinc-400">
          No page contains all of your words. Showing pages with most of them.
        </p>
      )}

      {response.total === 0 ? (
        <div className="flex flex-col gap-2 py-6">
          <p>
            No pages match <span className="font-semibold">{query}</span>.
          </p>
          <ul className="list-disc pl-5 text-sm text-zinc-600 dark:text-zinc-400">
            <li>Every word must start a word on the page. Try fewer words.</li>
            <li>Try the start of a word, such as &ldquo;archit&rdquo; for architecture.</li>
          </ul>
        </div>
      ) : (
        <ol className="flex flex-col gap-7">
          {response.results.map((result) => (
            <ResultItem key={result.line} result={result} words={words} />
          ))}
        </ol>
      )}

      {pageCount > 1 && (
        <nav aria-label="Result pages" className="flex flex-wrap items-center gap-2 py-4 text-sm">
          {page > 1 && (
            <Link
              href={href(query, index, page - 1)}
              className="rounded-full border border-black/[.12] px-3 py-1 dark:border-white/[.2]"
            >
              Previous
            </Link>
          )}
          {Array.from({ length: pageCount }, (_, i) => i + 1)
            .filter((n) => Math.abs(n - page) <= 4 || n === 1 || n === pageCount)
            .map((n) =>
              n === page ? (
                <span
                  key={n}
                  aria-current="page"
                  className="rounded-full bg-foreground px-3 py-1 text-background tabular-nums"
                >
                  {n}
                </span>
              ) : (
                <Link
                  key={n}
                  href={href(query, index, n)}
                  className="rounded-full px-3 py-1 tabular-nums hover:bg-black/[.04] dark:hover:bg-white/[.06]"
                >
                  {n}
                </Link>
              ),
            )}
          {page < pageCount && (
            <Link
              href={href(query, index, page + 1)}
              className="rounded-full border border-black/[.12] px-3 py-1 dark:border-white/[.2]"
            >
              Next
            </Link>
          )}
        </nav>
      )}
    </>
  );
}

function ResultItem({ result, words }: { result: SearchResult; words: string[] }) {
  // Pages from the web are stored as "title: description".
  const split = /^(.+?):\s(.+)$/u.exec(result.text);
  const title = <Highlighted text={split ? split[1] : result.text} words={words} />;
  const snippet = split?.[2];
  return (
    <li className="flex max-w-3xl flex-col gap-1">
      {result.url && <p className="truncate text-sm text-zinc-500">{breadcrumb(result.url)}</p>}
      <h2 className="text-lg leading-snug">
        {result.url && /^https?:\/\//i.test(result.url) ? (
          <a
            href={result.url}
            target="_blank"
            rel="noopener noreferrer"
            className="text-blue-700 underline-offset-2 hover:underline dark:text-blue-400"
          >
            {title}
          </a>
        ) : (
          title
        )}
      </h2>
      {snippet && (
        <p className="line-clamp-2 text-sm text-zinc-700 dark:text-zinc-300">
          <Highlighted text={snippet} words={words} />
        </p>
      )}
      <p className="flex flex-wrap items-center gap-x-2 gap-y-1 text-sm text-zinc-600 dark:text-zinc-400">
        {result.phrase && words.length > 1 && (
          <span className="rounded bg-emerald-100 px-1.5 py-0.5 text-xs font-medium text-emerald-800 dark:bg-emerald-400/15 dark:text-emerald-300">
            Exact phrase
          </span>
        )}
        <span className="text-zinc-500">KWIC:</span>
        {result.matches.slice(0, SHOWN_MATCHES).map((m, i, shown) => (
          <span key={m.id} className="max-w-full truncate">
            <span className="font-semibold text-foreground">{m.keyword}</span> {m.context}
            {i < shown.length - 1 && <span className="text-zinc-400"> · </span>}
          </span>
        ))}
        {result.matches.length > SHOWN_MATCHES && (
          <span className="text-zinc-400">+{result.matches.length - SHOWN_MATCHES} more</span>
        )}
        <span className="text-zinc-400">· line {result.line}</span>
      </p>
      {result.missing.length > 0 && (
        <p className="text-sm text-zinc-500">
          Missing:{" "}
          {result.missing.map((w) => (
            <s key={w} className="mr-1.5">
              {w}
            </s>
          ))}
        </p>
      )}
    </li>
  );
}

/** The text with every word that starts with a search word in bold. */
function Highlighted({ text, words }: { text: string; words: string[] }) {
  return (
    <>
      {text.split(/(\s+)/u).map((part, i) =>
        words.some((w) => part.toLowerCase().startsWith(w)) ? (
          <strong key={i} className="font-semibold">
            {part}
          </strong>
        ) : (
          part
        ),
      )}
    </>
  );
}

function breadcrumb(url: string): string {
  try {
    const { hostname, pathname } = new URL(url);
    const parts = pathname.split("/").filter(Boolean).map((p) => {
      try {
        return decodeURIComponent(p);
      } catch {
        return p;
      }
    });
    return [hostname, ...parts].join(" › ");
  } catch {
    return url;
  }
}

function OwnIndexNotice({ query }: { query: string }) {
  return (
    <p className="rounded-lg bg-amber-100 px-3 py-2 text-sm text-amber-900 dark:bg-amber-400/15 dark:text-amber-200">
      Searching the pages you indexed in the{" "}
      <Link href="/indexer" className="underline underline-offset-2">
        Indexer
      </Link>
      .{" "}
      <Link href={href(query, "")} className="font-medium underline underline-offset-2">
        Search the web index instead
      </Link>
    </p>
  );
}

function Notice({ children }: { children: React.ReactNode }) {
  return (
    <div
      role="alert"
      className="max-w-2xl rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800 dark:border-red-400/30 dark:bg-red-400/10 dark:text-red-300"
    >
      {children}
    </div>
  );
}

/** "A", "A and B", or "A, B, and C". */
function listNames(names: string[]): string {
  if (names.length <= 1) return names.join("");
  return `${names.slice(0, -1).join(", ")}${names.length > 2 ? "," : ""} and ${names[names.length - 1]}`;
}

function Wordmark() {
  return (
    <>
      Quick<span className="text-blue-600 dark:text-blue-400">dex</span>
    </>
  );
}
