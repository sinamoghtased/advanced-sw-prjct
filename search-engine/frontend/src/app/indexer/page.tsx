import type { Metadata } from "next";
import IndexerApp from "./indexer-app";

export const metadata: Metadata = {
  title: "Indexer · Quickdex",
  description:
    "Build a KWIC index from your own text and watch its circular shifts and alphabetized entries update line by line.",
};

export default function IndexerPage() {
  return (
    <main className="mx-auto flex w-full max-w-5xl flex-1 flex-col gap-10 px-4 py-12 sm:px-6">
      <header className="flex flex-col gap-3">
        <p className="text-sm font-medium uppercase tracking-widest text-zinc-500">
          How the search engine works
        </p>
        <h1 className="text-3xl font-semibold tracking-tight sm:text-4xl">
          Build your own index.
        </h1>
        <p className="max-w-2xl text-zinc-600 dark:text-zinc-400">
          The search engine runs on a KWIC index. Enter your own pages, one per line, and watch
          Quickdex generate every circular shift and merge it into the alphabetized index after
          each line. Then search your pages in the search engine.
        </p>
      </header>
      <IndexerApp />
    </main>
  );
}
