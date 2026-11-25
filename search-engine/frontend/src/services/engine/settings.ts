/** The search settings a user can change on the Search page (FR025 to FR027, FR030, FR031). */

export const SEARCH_MODES = ["or", "and", "not"] as const;
export const RESULT_ORDERS = ["relevance", "alphabetical", "clicks", "bid"] as const;
export const PAGE_SIZES = [5, 10, 20, 50] as const;

export type SearchMode = (typeof SEARCH_MODES)[number];
export type ResultOrder = (typeof RESULT_ORDERS)[number];

export type SearchSettings = {
  mode: SearchMode;
  caseSensitive: boolean;
  /** Symbols to leave out when matching, such as "-&". */
  symbols: string;
  order: ResultOrder;
  size: number;
};

export const DEFAULT_SETTINGS: SearchSettings = {
  mode: "or",
  caseSensitive: false,
  symbols: "",
  order: "relevance",
  size: 10,
};

export const MODE_LABELS: Record<SearchMode, string> = {
  or: "Any word (OR)",
  and: "All words (AND)",
  not: "First word, not the others (NOT)",
};

export const ORDER_LABELS: Record<ResultOrder, string> = {
  relevance: "Best match",
  alphabetical: "A to Z",
  clicks: "Most clicked",
  bid: "Highest bid",
};

type Params = { get(name: string): string | null };

/** Reads the settings from a URL's query, ignoring any value that is not allowed. */
export function settingsFromParams(params: Params): SearchSettings {
  const mode = params.get("mode")?.toLowerCase();
  const order = params.get("order")?.toLowerCase();
  const size = Number.parseInt(params.get("size") ?? "", 10);
  return {
    mode: SEARCH_MODES.find((m) => m === mode) ?? DEFAULT_SETTINGS.mode,
    caseSensitive: params.get("case") === "true",
    symbols: (params.get("symbols") ?? "").replace(/\s+/gu, "").slice(0, 50),
    order: RESULT_ORDERS.find((o) => o === order) ?? DEFAULT_SETTINGS.order,
    size: PAGE_SIZES.find((s) => s === size) ?? DEFAULT_SETTINGS.size,
  };
}

/** Adds the settings that differ from the defaults to a URL's query. */
export function addSettingsToParams(params: URLSearchParams, settings: SearchSettings): void {
  if (settings.mode !== DEFAULT_SETTINGS.mode) params.set("mode", settings.mode);
  if (settings.caseSensitive) params.set("case", "true");
  if (settings.symbols !== "") params.set("symbols", settings.symbols);
  if (settings.order !== DEFAULT_SETTINGS.order) params.set("order", settings.order);
  if (settings.size !== DEFAULT_SETTINGS.size) params.set("size", String(settings.size));
}

/** The parameters the engine's /search endpoint takes for these settings. */
export function engineParams(settings: SearchSettings): Record<string, string> {
  return {
    mode: settings.mode,
    case: settings.caseSensitive ? "true" : "",
    symbols: settings.symbols,
    order: settings.order,
  };
}
