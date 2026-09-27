/**
 * Research papers (arXiv API). Free, no key. arXiv's terms allow no more than one request
 * every three seconds on a single connection, so a search that comes sooner skips arXiv.
 * https://info.arxiv.org/help/api/tou.html
 */
import { CACHE_SECONDS, TIMEOUT_MS, USER_AGENT, type WebPage, type WebSource } from "./page";

const RESULTS = 5;
const MIN_INTERVAL_MS = 3000;

let lastRequest = 0;
let busy = false;

export const arxiv: WebSource = {
  name: "arXiv",
  async search(query: string): Promise<WebPage[]> {
    if (busy || Date.now() - lastRequest < MIN_INTERVAL_MS) return [];
    const terms = query.trim().split(/\s+/u).filter(Boolean);
    if (terms.length === 0) return [];
    busy = true;
    lastRequest = Date.now();
    try {
      const searchQuery = terms.map((t) => `all:${encodeURIComponent(t)}`).join("+AND+");
      const response = await fetch(
        `https://export.arxiv.org/api/query?search_query=${searchQuery}&max_results=${RESULTS}`,
        {
          headers: { "User-Agent": USER_AGENT },
          next: { revalidate: CACHE_SECONDS },
          signal: AbortSignal.timeout(TIMEOUT_MS),
        },
      );
      if (!response.ok) throw new Error(`arXiv answered ${response.status}`);
      return parseEntries(await response.text());
    } finally {
      busy = false;
      lastRequest = Date.now();
    }
  },
};

/** Reads the papers from arXiv's Atom feed. */
function parseEntries(atom: string): WebPage[] {
  const pages: WebPage[] = [];
  for (const [, entry] of atom.matchAll(/<entry>([\s\S]*?)<\/entry>/g)) {
    const id = /<id>([^<]+)<\/id>/.exec(entry)?.[1]?.trim();
    const title = /<title>([\s\S]*?)<\/title>/.exec(entry)?.[1] ?? "";
    const summary = /<summary>([\s\S]*?)<\/summary>/.exec(entry)?.[1] ?? "";
    if (id) {
      pages.push({
        url: id.replace(/^http:/, "https:"),
        title: title.replace(/\s+/g, " ").trim(),
        description: summary.replace(/\s+/g, " ").trim(),
      });
    }
  }
  return pages;
}
