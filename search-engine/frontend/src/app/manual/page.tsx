import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = {
  title: "Manual · Quickdex",
  description: "How to use Quickdex: searching, suggestions, results, and the Indexer tab.",
};

const results = [
  ["Ranking", "Exact title, then exact phrase, then most matching words, then earliest match, then alphabetical."],
  ["Exact phrase badge", "Your words matched together, in order, on the page."],
  ["KWIC matches", "The rotated context around your keyword; “+N more” shows there are others."],
  ["Missing", "Words struck through are ones no single page fully contains."],
];

const indexerSteps = [
  ["Enter text", "Type or paste lines directly. One line = one entry."],
  ["Create index", "Watch the circular shifts table fill and the alphabetized table grow, line by line."],
  ["Slow motion", "Optionally build one line at a time instead of all at once."],
  ["Filter", "Find entries in the built alphabetized table by keyword."],
  ["Copy results", "Copy the circular shifts or alphabetized index as tab-separated text."],
  ["Search it", "Open your own pages in the same search UI used on the home page."],
];

const limitations = [
  "Search matches are prefix matches on whole words, not substring or fuzzy matches.",
  "Live web sources exist in the code but are off by default; Phase I answers only from corpus.txt.",
  "Indexes are in memory only — restarting the backend clears anything built in the Indexer tab.",
  "The Indexer accepts up to 10,000 Unicode code points, including whitespace.",
];

export default function ManualPage() {
  return (
    <main className="mx-auto flex w-full max-w-5xl flex-1 flex-col gap-16 px-4 py-16 sm:px-6">
      <section className="flex flex-col gap-6">
        <p className="text-sm font-medium uppercase tracking-widest text-zinc-500">
          User manual &middot; preliminary
        </p>
        <h1 className="max-w-2xl text-4xl font-semibold leading-tight tracking-tight sm:text-5xl">
          How to use Quickdex.
        </h1>
        <p className="max-w-2xl text-lg leading-8 text-zinc-600 dark:text-zinc-400">
          Quickdex is a search engine built on a Key Word In Context (KWIC) index: every word is
          rotated to the front once, and those rotations are sorted alphabetically. That&rsquo;s
          why a search word must <em>start</em> a word on the page, not just appear inside it. In
          Phase I, results come only from the engine&rsquo;s own built-in index &mdash; no live
          internet lookups.
        </p>
        <Link
          href="/"
          className="flex h-11 w-fit items-center justify-center rounded-full bg-foreground px-6 text-sm font-medium text-background transition-colors hover:bg-[#383838] dark:hover:bg-[#ccc]"
        >
          Try the search engine
        </Link>
      </section>

      <section className="flex flex-col gap-4">
        <h2 className="text-sm font-medium uppercase tracking-widest text-zinc-500">
          1. Searching (home page)
        </h2>
        <ol className="max-w-2xl list-decimal space-y-2 pl-5 text-zinc-600 dark:text-zinc-400">
          <li>Type a word or a few words into the search box.</li>
          <li>
            Pick a <strong className="text-foreground">suggestion</strong> from the dropdown
            (<kbd className="rounded border border-black/[.15] px-1 text-xs dark:border-white/[.25]">&darr;</kbd>{" "}
            /{" "}
            <kbd className="rounded border border-black/[.15] px-1 text-xs dark:border-white/[.25]">&uarr;</kbd>
            {" "}to move, <kbd className="rounded border border-black/[.15] px-1 text-xs dark:border-white/[.25]">Enter</kbd> to pick,{" "}
            <kbd className="rounded border border-black/[.15] px-1 text-xs dark:border-white/[.25]">Esc</kbd> to close), or press{" "}
            <kbd className="rounded border border-black/[.15] px-1 text-xs dark:border-white/[.25]">Enter</kbd> to search what you typed.
          </li>
          <li>Page through results with Previous / Next or the page numbers.</li>
        </ol>
        <ul className="grid gap-3 sm:grid-cols-2">
          {results.map(([name, job]) => (
            <li
              key={name}
              className="rounded-lg border border-black/[.08] p-4 dark:border-white/[.145]"
            >
              <p className="font-medium">{name}</p>
              <p className="text-sm text-zinc-600 dark:text-zinc-400">{job}</p>
            </li>
          ))}
        </ul>
      </section>

      <section className="flex flex-col gap-4">
        <h2 className="text-sm font-medium uppercase tracking-widest text-zinc-500">
          2. Building your own index (the Indexer tab)
        </h2>
        <p className="max-w-2xl text-zinc-600 dark:text-zinc-400">
          The{" "}
          <Link href="/indexer" className="underline underline-offset-2">
            Indexer
          </Link>{" "}
          builds a KWIC index from your own text using the same pipeline as the home page, but
          visibly, line by line.
        </p>
        <ol className="grid gap-3 sm:grid-cols-2">
          {indexerSteps.map(([name, job]) => (
            <li
              key={name}
              className="rounded-lg border border-black/[.08] p-4 dark:border-white/[.145]"
            >
              <p className="font-medium">{name}</p>
              <p className="text-sm text-zinc-600 dark:text-zinc-400">{job}</p>
            </li>
          ))}
        </ol>
      </section>

      <section className="flex flex-col gap-4">
        <h2 className="text-sm font-medium uppercase tracking-widest text-zinc-500">
          3. Known limitations (Phase I)
        </h2>
        <ul className="max-w-2xl list-disc space-y-2 pl-5 text-zinc-600 dark:text-zinc-400">
          {limitations.map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ul>
      </section>

      <section className="flex flex-col gap-4">
        <h2 className="text-sm font-medium uppercase tracking-widest text-zinc-500">
          Getting help
        </h2>
        <p className="max-w-2xl text-zinc-600 dark:text-zinc-400">
          If the app says the backend isn&rsquo;t running, follow{" "}
          <a
            href="https://github.com/sinamoghtased/advanced-sw-prjct/blob/main/search-engine/README.md#run-locally"
            target="_blank"
            rel="noopener noreferrer"
            className="underline underline-offset-2"
          >
            Run locally
          </a>{" "}
          in the README. For anything else, see the{" "}
          <Link href="/about" className="underline underline-offset-2">
            About
          </Link>{" "}
          page for the team, or open an issue on{" "}
          <a
            href="https://github.com/sinamoghtased/advanced-sw-prjct"
            target="_blank"
            rel="noopener noreferrer"
            className="underline underline-offset-2"
          >
            GitHub
          </a>
          .
        </p>
      </section>
    </main>
  );
}
