/**
 * Brave Search API: results from the whole web. Needs a key in BRAVE_API_KEY; new accounts
 * get a monthly credit, then pay per request.
 * https://brave.com/search/api/
 */
import { CACHE_SECONDS, TIMEOUT_MS, type WebPage, type WebSource } from "./page";

const RESULTS = 20;

export const brave: WebSource = {
  name: "Brave Search",
  async search(query: string): Promise<WebPage[]> {
    const url = `https://api.search.brave.com/res/v1/web/search?q=${encodeURIComponent(query)}&count=${RESULTS}`;
    const response = await fetch(url, {
      headers: { Accept: "application/json", "X-Subscription-Token": process.env.BRAVE_API_KEY ?? "" },
      next: { revalidate: CACHE_SECONDS },
      signal: AbortSignal.timeout(TIMEOUT_MS),
    });
    if (!response.ok) throw new Error(`Brave answered ${response.status}`);
    const data = (await response.json()) as {
      web?: { results?: { url: string; title: string; description?: string }[] };
    };
    return (data.web?.results ?? []).map((r) => ({
      url: r.url,
      title: r.title,
      description: r.description ?? "",
    }));
  },
};
