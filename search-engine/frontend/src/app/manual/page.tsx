import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = {
  title: "Manual · Quickdex",
  description: "How to use Cyberminer on Quickdex: searching, search settings, suggestions, admin tools, and the Indexer tab.",
};

const results = [
  ["Best match order", "Exact title, then exact phrase, then most matching words, then earliest match, then alphabetical. This is the default order."],
  ["Exact phrase badge", "Your words matched together, in order, on the page."],
  ["KWIC matches", "The rotated context around your keyword; “+N more” shows there are others."],
  ["Missing", "Words struck through are ones no single page fully contains."],
];

const settings = [
  ["Search mode", "Any word (OR) is the default and finds pages with at least one of your words. All words (AND) needs every word. First word, not the others (NOT) finds pages with your first word and none of the rest; with a single word it finds the pages that do not have it."],
  ["Match case", "Off by default. When on, “Java” does not find “java”."],
  ["Symbols to ignore", "List symbols such as - or & to leave out when matching. With - ignored, “selfdriving” finds “self-driving”."],
  ["Order results by", "Best match, A to Z, most clicked, or highest bid. The order never changes which pages match, only their sequence."],
  ["Results per page", "Show 5, 10, 20 or 50 results at a time, and move between pages with Previous / Next or the page numbers."],
];

const adminSteps = [
  ["Open the tools", "Press Admin tools above the results. Each result then shows a bid box and a Delete button."],
  ["Delete a page", "Press Delete, then confirm. The page, its description and all its shifts leave the index. Cancel keeps the page."],
  ["Set a bid", "Type a whole number and press Set bid. Pages with higher bids come first when results are ordered by highest bid."],
  ["Admin key", "If the server has an admin key, you are asked to type it in. It is kept for this browser tab only."],
];

const indexerSteps = [
  ["Enter text", "Type or paste lines directly. One line = one entry."],
  ["Create index", "Watch the circular shifts, the shifts left after noise-word filtering, and the alphabetized index fill in, line by line."],
  ["Noise words", "Shifts that start with a common word such as “a”, “the” or “of” are removed from the index."],
  ["Slow motion", "Optionally build one line at a time instead of all at once."],
  ["Filter", "Find entries in the built alphabetized table by keyword."],
  ["Copy results", "Copy the circular shifts or alphabetized index as tab-separated text."],
  ["Search it", "Open your own pages in the same search UI used on the home page."],
];

const limitations = [
  "Search matches are prefix matches on words, not substring matches. Typos are corrected only in the suggestions while you type.",
  "A search made only of common words, such as “the”, finds nothing, because the index leaves out shifts that start with them.",
  "Online sources are queried alongside the built-in index only when they are turned on; they are off by default.",
  "Pages must have a URL ending in .edu, .com, .org or .net and a description. Other pages are rejected.",
  "Everything is held in memory — restarting the backend clears indexes built in the Indexer tab, and also forgets deleted pages, clicks and bids.",
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
          Cyberminer is a web search engine built on Quickdex&rsquo;s Key Word In Context (KWIC)
          index: every word is rotated to the front once, and those rotations are sorted
          alphabetically. That&rsquo;s why a search word must <em>start</em> a word on the page, not
          just appear inside it. Results come from the engine&rsquo;s own built-in index; online
          sources can be added alongside it when they are turned on.
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
          <li>Page through results with Previous / Next or the page numbers. Click a result to open its web site.</li>
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
          2. Search settings
        </h2>
        <p className="max-w-2xl text-zinc-600 dark:text-zinc-400">
          Press <strong className="text-foreground">Search settings</strong> under the search box.
          Changing a setting runs the search you are looking at again, and the settings stay in the
          address so you can share a link.
        </p>
        <ul className="grid gap-3 sm:grid-cols-2">
          {settings.map(([name, job]) => (
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
          3. Admin tools
        </h2>
        <p className="max-w-2xl text-zinc-600 dark:text-zinc-400">
          Administrators keep the index up to date. Anyone who has the admin key is an administrator;
          nobody else can delete pages or set bids.
        </p>
        <ol className="grid gap-3 sm:grid-cols-2">
          {adminSteps.map(([name, job]) => (
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
          4. Building your own index (the Indexer tab)
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
          5. Known limitations
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
