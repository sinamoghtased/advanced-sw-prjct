/** A page found by an online source, and how it becomes one line of engine input. */

export type WebPage = { url: string; title: string; description: string };

/** An online source of web pages. */
export type WebSource = {
  /** The source's name, shown to users. */
  name: string;
  /** Finds pages that match a search. Returns an empty list when it has to skip a search. */
  search(query: string): Promise<WebPage[]>;
};

export const USER_AGENT =
  "QuickdexSearch/0.1 (https://github.com/sinamoghtased/advanced-sw-prjct; CS 6362 course project)";

/** How long answers from online sources are cached. */
export const CACHE_SECONDS = 24 * 60 * 60;

/** How long to wait for an online source. */
export const TIMEOUT_MS = 3500;

const MAX_WORDS_PER_PAGE = 30;

/** One line for the engine: the URL, then the title and description, as plain words. */
export function toLine(page: WebPage): string | null {
  if (!/^https?:\/\/\S+$/i.test(page.url)) return null;
  const title = plainText(page.title);
  const description = plainText(page.description);
  const text = description ? `${title}: ${description}` : title;
  const words = text.split(/\s+/u).filter(Boolean).slice(0, MAX_WORDS_PER_PAGE);
  return words.length > 0 ? `${page.url} ${words.join(" ")}` : null;
}

/** Removes HTML tags and decodes common entities. */
export function plainText(html: string): string {
  return html
    .replace(/<[^>]*>/g, " ")
    .replace(/&quot;/g, '"')
    .replace(/&#0?39;|&apos;/g, "'")
    .replace(/&#(\d+);/g, (_, code: string) => String.fromCodePoint(Number(code)))
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&nbsp;/g, " ")
    .replace(/&amp;/g, "&")
    .replace(/\s+/gu, " ")
    .trim();
}
