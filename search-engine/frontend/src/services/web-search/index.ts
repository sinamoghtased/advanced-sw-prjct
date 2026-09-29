/**
 * Fetches pages from online sources and adds them to the backend's web index.
 *
 * The online sources are part of the Input Medium: each search first asks every enabled
 * source for matching pages, turns each page into one "URL title: description" line, and
 * posts the lines to the backend, which adds new ones to the KWIC index one line at a time.
 * The backend's SearchEngine then answers the search from its own index.
 *
 * QUICKDEX_WEB_SOURCES lists the sources to use, separated by commas: wikipedia, hackernews,
 * stackexchange, arxiv, and (with BRAVE_API_KEY) brave. Phase I answers only from the
 * backend's own corpus.txt index, so the default is "off"; set QUICKDEX_WEB_SOURCES to opt
 * back into live results.
 */
import { postToEngine } from "@/services/engine/backend";
import { arxiv } from "./arxiv";
import { brave } from "./brave";
import { hackerNews } from "./hackernews";
import { toLine, type WebSource } from "./page";
import { stackExchange } from "./stackexchange";
import { wikipedia, wikipediaTitles } from "./wikipedia";

const ALL_SOURCES: Record<string, WebSource> = {
  wikipedia,
  hackernews: hackerNews,
  stackexchange: stackExchange,
  arxiv,
  brave,
};

function enabledSources(): WebSource[] {
  const setting = process.env.QUICKDEX_WEB_SOURCES?.trim().toLowerCase();
  if (!setting || setting === "off") return [];
  const names = setting.split(",").map((s) => s.trim());
  return names
    .filter((name) => name !== "brave" || process.env.BRAVE_API_KEY)
    .map((name) => ALL_SOURCES[name])
    .filter((source): source is WebSource => source !== undefined);
}

/** The names of the enabled online sources. */
export function webSourceNames(): string[] {
  return enabledSources().map((s) => s.name);
}

export type IngestResult = { added: number; used: string[]; failed: string[] } | null;

/**
 * Asks every enabled source for pages that match a search and adds the new ones to the
 * backend's web index. Returns null when no online source is enabled.
 */
export async function addWebPages(query: string): Promise<IngestResult> {
  const sources = enabledSources();
  if (sources.length === 0) return null;

  const answers = await Promise.allSettled(sources.map((s) => s.search(query)));
  const lines: string[] = [];
  const used: string[] = [];
  const failed: string[] = [];
  answers.forEach((answer, i) => {
    if (answer.status === "rejected") {
      failed.push(sources[i].name);
      return;
    }
    const found = answer.value.map(toLine).filter((line): line is string => line !== null);
    if (found.length > 0) used.push(sources[i].name);
    lines.push(...found);
  });
  if (lines.length === 0) return { added: 0, used, failed };

  const reply = await postToEngine<{ added: number }>("/pages", lines.join("\n"));
  if (!reply.ok) return { added: 0, used: [], failed: sources.map((s) => s.name) };
  return { added: reply.data.added, used, failed };
}

/** Suggests searches from the online sources: Wikipedia page titles. */
export async function webSuggestions(typed: string, limit: number): Promise<string[]> {
  if (typed.trim() === "" || !enabledSources().includes(wikipedia)) return [];
  return wikipediaTitles(typed, limit);
}
