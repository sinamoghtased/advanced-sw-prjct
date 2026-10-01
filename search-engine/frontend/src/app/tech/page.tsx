import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Tech · Quickdex",
  description: "Technologies used in Quickdex, mapped to the architecture rationale behind each one.",
};

const rows: [string, string, string][] = [
  ["Plain JDK HttpServer, no framework", "Information hiding", "Nothing auto-exposes your classes; only what you make public is public."],
  ["Narrow public methods per component", "Modifiability of algorithms", "One or two methods per class — swap an internal algorithm, nothing else breaks."],
  ["InputMedium / OutputMedium interfaces", "Reusability", "Same pipeline classes run unmodified for both the CLI and the web server."],
  ["Streaming NDJSON over one HTTP response", "Output timing / incremental merge", "One JSON line out per line processed — the constraint, implemented directly."],
  ["Next.js API routes as a thin proxy", "Modifiability of data", "Frontend only sees JSON; backend storage can change freely behind it."],
  ["JUnit 5, one test class per component", "Reusability + information hiding", "Proves the separation is real — isolated components are what makes isolated tests possible."],
  ["Separate Maven / npm builds", "Reusability, at the deployment level", "Two independent toolchains mirror the component separation in code."],
  ["Docker multi-stage build", "Enhanceability (deployment)", "Same image runs on any container host, no code changes."],
  ["Vercel + Render, one env var", "Modifiability, at the infra level", "Swap either host or the backend's language entirely; only the HTTP contract matters."],
  ["TypeScript, Tailwind CSS", "—", "General frontend tooling, not part of the ADT rationale."],
];

export default function TechPage() {
  return (
    <main className="mx-auto flex w-full max-w-5xl flex-1 flex-col gap-8 px-4 py-16 sm:px-6">
      <div className="flex flex-col gap-3">
        <p className="text-sm font-medium uppercase tracking-widest text-zinc-500">
          Architecture rationale
        </p>
        <h1 className="text-3xl font-semibold tracking-tight sm:text-4xl">
          Technologies mapped to the rationale
        </h1>
        <p className="max-w-2xl text-zinc-600 dark:text-zinc-400">
          Every technology choice in Quickdex traces back to a specific architectural goal.
        </p>
      </div>

      <div className="overflow-x-auto rounded-lg border border-black/[.08] dark:border-white/[.145]">
        <table className="w-full text-left text-sm">
          <thead className="bg-black/[.03] dark:bg-white/[.06]">
            <tr>
              <th className="whitespace-nowrap px-4 py-3 font-medium">Technology</th>
              <th className="whitespace-nowrap px-4 py-3 font-medium">Maps to</th>
              <th className="px-4 py-3 font-medium">Why</th>
            </tr>
          </thead>
          <tbody>
            {rows.map(([tech, mapsTo, why]) => (
              <tr key={tech} className="border-t border-black/[.08] dark:border-white/[.145]">
                <td className="whitespace-nowrap px-4 py-3 font-mono text-xs">{tech}</td>
                <td className="whitespace-nowrap px-4 py-3 text-zinc-600 dark:text-zinc-400">{mapsTo}</td>
                <td className="px-4 py-3 text-zinc-600 dark:text-zinc-400">{why}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </main>
  );
}
