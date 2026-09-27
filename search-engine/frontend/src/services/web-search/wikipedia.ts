/**
 * Wikipedia search (Wikimedia REST API). Free, no key; 200 requests a minute for clients
 * that send a descriptive User-Agent.
 * https://www.mediawiki.org/wiki/API:REST_API/Reference
 */
import { CACHE_SECONDS, TIMEOUT_MS, USER_AGENT, type WebPage, type WebSource } from "./page";

const RESULTS = 10;
const HEADERS = { "User-Agent": USER_AGENT, "Api-User-Agent": USER_AGENT };

export const wikipedia: WebSource = {
  name: "Wikipedia",
  async search(query: string): Promise<WebPage[]> {
    const url = `https://en.wikipedia.org/w/rest.php/v1/search/page?q=${encodeURIComponent(query)}&limit=${RESULTS}`;
    const response = await fetch(url, {
      headers: HEADERS,
      next: { revalidate: CACHE_SECONDS },
      signal: AbortSignal.timeout(TIMEOUT_MS),
    });
    if (!response.ok) throw new Error(`Wikipedia answered ${response.status}`);
    const data = (await response.json()) as {
      pages?: { key: string; title: string; description: string | null; excerpt: string | null }[];
    };
    return (data.pages ?? []).map((p) => ({
      url: `https://en.wikipedia.org/wiki/${encodeURIComponent(p.key)}`,
      title: p.title,
      description: p.description ?? p.excerpt ?? "",
    }));
  },
};

/** Wikipedia page titles that start with what the user typed, for search suggestions. */
export async function wikipediaTitles(typed: string, limit: number): Promise<string[]> {
  const url = `https://en.wikipedia.org/w/rest.php/v1/search/title?q=${encodeURIComponent(typed)}&limit=${limit}`;
  try {
    const response = await fetch(url, {
      headers: HEADERS,
      next: { revalidate: CACHE_SECONDS },
      signal: AbortSignal.timeout(1200),
    });
    if (!response.ok) return [];
    const data = (await response.json()) as { pages?: { title: string }[] };
    return (data.pages ?? []).map((p) => p.title.toLowerCase());
  } catch {
    return [];
  }
}
