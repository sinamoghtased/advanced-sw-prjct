import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = {
  title: "About · Quickdex",
  description: "Quickdex: a KWIC indexing search engine built for CS 6362 at UT Dallas.",
};

const team = [
  { name: "Yasin Sazid", email: "yasin.sazid@utdallas.edu" },
  { name: "Alessandro Botta", email: "alessandro.botta@utdallas.edu" },
  { name: "Sina Moghtased", email: "sina.moghtased@utdallas.edu" },
];

const components = [
  ["MasterControl", "Runs the steps below once per line."],
  ["Input", "Reads each line and stores it."],
  ["LineStorage", "Keeps the characters and words of every line."],
  ["CircularShift", "Generates every circular shift of the line."],
  ["Alphabetizer", "Merges the new shifts into the alphabetized index."],
  ["Output", "Shows the updated index after every line."],
  ["SearchEngine", "Finds the pages whose words match your search."],
];

export default function AboutPage() {
  return (
    <main className="mx-auto flex w-full max-w-5xl flex-1 flex-col gap-16 px-4 py-16 sm:px-6">
      <section className="flex flex-col gap-6">
        <p className="text-sm font-medium uppercase tracking-widest text-zinc-500">
          CS 6362 &middot; Advanced Software Architecture
        </p>
        <h1 className="max-w-2xl text-4xl font-semibold leading-tight tracking-tight sm:text-5xl">
          Quickdex: a KWIC indexing search engine.
        </h1>
        <p className="max-w-2xl text-lg leading-8 text-zinc-600 dark:text-zinc-400">
          An object-oriented Key Word In Context system that generates circular word shifts
          from input text and displays them in alphabetical order, forming the core of a
          web-based search engine.
        </p>
        <div className="flex flex-col gap-3 sm:flex-row">
          <Link
            href="/"
            className="flex h-11 items-center justify-center rounded-full bg-foreground px-6 text-sm font-medium text-background transition-colors hover:bg-[#383838] dark:hover:bg-[#ccc]"
          >
            Try the search engine
          </Link>
          <a
            href="/project-plan.pdf"
            target="_blank"
            rel="noopener noreferrer"
            className="flex h-11 items-center justify-center rounded-full border border-black/[.12] px-6 text-sm font-medium transition-colors hover:bg-black/[.04] dark:border-white/[.2] dark:hover:bg-white/[.06]"
          >
            Read the project plan
          </a>
        </div>
      </section>

      <section className="flex flex-col gap-4">
        <h2 className="text-sm font-medium uppercase tracking-widest text-zinc-500">
          How it works
        </h2>
        <p className="max-w-2xl text-zinc-600 dark:text-zinc-400">
          Quickdex uses the Abstract Data Type architecture: each component hides its data and
          algorithms behind a few public operations. Every page added to the index goes through
          these components, one line at a time. You can watch it happen in the{" "}
          <Link href="/indexer" className="underline underline-offset-2">
            Indexer
          </Link>
          .
        </p>
        <ol className="grid gap-3 sm:grid-cols-2">
          {components.map(([name, job]) => (
            <li
              key={name}
              className="rounded-lg border border-black/[.08] p-4 dark:border-white/[.145]"
            >
              <p className="font-mono text-sm font-medium">{name}</p>
              <p className="text-sm text-zinc-600 dark:text-zinc-400">{job}</p>
            </li>
          ))}
        </ol>
      </section>

      <section className="flex flex-col gap-4">
        <h2 className="text-sm font-medium uppercase tracking-widest text-zinc-500">Team</h2>
        <ul className="grid gap-4 sm:grid-cols-3">
          {team.map((member) => (
            <li
              key={member.email}
              className="rounded-lg border border-black/[.08] p-4 dark:border-white/[.145]"
            >
              <p className="font-medium">{member.name}</p>
              <a
                href={`mailto:${member.email}`}
                className="text-sm text-zinc-600 hover:underline dark:text-zinc-400"
              >
                {member.email}
              </a>
            </li>
          ))}
        </ul>
      </section>
    </main>
  );
}
