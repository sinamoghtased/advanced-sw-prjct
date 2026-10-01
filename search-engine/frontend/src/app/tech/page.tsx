import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Tech · Quickdex",
  description: "Technologies used in Quickdex, mapped to the architecture rationale behind each one.",
};

const rows: [string, string, string][] = [
  ["Plain JDK HttpServer", "Information hiding", "Only public methods are exposed."],
  ["Narrow public methods", "Modifiability", "Swap an algorithm, nothing else breaks."],
  ["InputMedium / OutputMedium", "Reusability", "Same pipeline for CLI and web."],
  ["Streaming NDJSON", "Output timing", "One JSON line per line processed."],
  ["Next.js API routes", "Modifiability", "Frontend only sees JSON."],
  ["JUnit 5 per component", "Reusability", "Isolated components, isolated tests."],
  ["Separate Maven / npm builds", "Reusability", "Independent toolchains per component."],
  ["Docker multi-stage build", "Enhanceability", "Runs on any container host."],
  ["Vercel + Render", "Modifiability", "Swap either host behind one env var."],
  ["TypeScript, Tailwind", "—", "General frontend tooling."],
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
