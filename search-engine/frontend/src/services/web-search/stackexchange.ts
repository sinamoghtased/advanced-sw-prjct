/**
 * Stack Overflow questions (Stack Exchange API 2.3). Free; 300 requests a day per IP address
 * without a key, or 10,000 a day with a free key in STACKEXCHANGE_KEY. The API may ask
 * callers to back off for a number of seconds, which is respected here.
 * https://api.stackexchange.com/docs
 */
import { CACHE_SECONDS, TIMEOUT_MS, USER_AGENT, type WebPage, type WebSource } from "./page";

const RESULTS = 10;

/** No requests are made before this time (ms since the epoch), after a backoff or throttle. */
let pausedUntil = 0;

export const stackExchange: WebSource = {
  name: "Stack Overflow",
  async search(query: string): Promise<WebPage[]> {
    if (Date.now() < pausedUntil) return [];
    const params = new URLSearchParams({
      order: "desc",
      sort: "relevance",
      q: query,
      site: "stackoverflow",
      pagesize: String(RESULTS),
    });
    if (process.env.STACKEXCHANGE_KEY) params.set("key", process.env.STACKEXCHANGE_KEY);
    const response = await fetch(`https://api.stackexchange.com/2.3/search/advanced?${params}`, {
      headers: { "User-Agent": USER_AGENT },
      next: { revalidate: CACHE_SECONDS },
      signal: AbortSignal.timeout(TIMEOUT_MS),
    });
    const data = (await response.json()) as {
      items?: { title: string; link: string; tags?: string[] }[];
      backoff?: number;
      quota_remaining?: number;
      error_message?: string;
    };
    if (data.backoff) pausedUntil = Date.now() + data.backoff * 1000;
    if (!response.ok) {
      // Throttled or out of quota: pause for an hour rather than keep asking.
      pausedUntil = Math.max(pausedUntil, Date.now() + 60 * 60 * 1000);
      throw new Error(data.error_message ?? `Stack Exchange answered ${response.status}`);
    }
    if (data.quota_remaining === 0) pausedUntil = Date.now() + 60 * 60 * 1000;
    return (data.items ?? []).map((item) => ({
      url: item.link,
      title: item.title,
      // Tags are useful keywords, such as "sorting" or "algorithm".
      description: (item.tags ?? []).join(" "),
    }));
  },
};
