/**
 * Hacker News search (Algolia HN Search API): articles from across the web that were posted
 * to Hacker News. Free, no key; 10,000 requests an hour per IP address.
 * https://hn.algolia.com/api
 */
import { CACHE_SECONDS, TIMEOUT_MS, USER_AGENT, type WebPage, type WebSource } from "./page";

const RESULTS = 10;

export const hackerNews: WebSource = {
  name: "Hacker News",
  async search(query: string): Promise<WebPage[]> {
    const url = `https://hn.algolia.com/api/v1/search?query=${encodeURIComponent(query)}&tags=story&hitsPerPage=${RESULTS}`;
    const response = await fetch(url, {
      headers: { "User-Agent": USER_AGENT },
      next: { revalidate: CACHE_SECONDS },
      signal: AbortSignal.timeout(TIMEOUT_MS),
    });
    if (!response.ok) throw new Error(`Hacker News answered ${response.status}`);
    const data = (await response.json()) as {
      hits?: { objectID: string; title: string | null; url: string | null }[];
    };
    return (data.hits ?? [])
      .filter((hit) => hit.title)
      .map((hit) => ({
        // Discussion-only posts have no article URL; link to the discussion instead.
        url: hit.url ?? `https://news.ycombinator.com/item?id=${hit.objectID}`,
        title: hit.title ?? "",
        description: "",
      }));
  },
};
