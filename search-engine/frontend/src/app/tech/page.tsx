import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Tech · Quickdex",
  description: "Technologies used in Quickdex, mapped to the architecture rationale behind each one.",
};

const rows: [string, string, string][] = [
  ["JDK HttpServer", "Information hiding", "No auto-exposed classes."],
  ["Narrow public methods", "Modifiability", "Swap internals safely."],
  ["InputMedium / OutputMedium", "Reusability", "One pipeline, two mediums."],
  ["Streaming NDJSON", "Output timing", "Line-by-line updates."],
  ["Next.js API routes", "Modifiability", "Frontend sees only JSON."],
  ["JUnit 5", "Reusability", "Tests per component."],
  ["Maven / npm", "Reusability", "Independent builds."],
  ["Docker", "Enhanceability", "Runs anywhere."],
  ["Vercel + Render", "Modifiability", "One env var swaps hosts."],
  ["TypeScript, Tailwind", "—", "Frontend tooling."],
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
          Each choice, mapped to the goal it serves.
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
