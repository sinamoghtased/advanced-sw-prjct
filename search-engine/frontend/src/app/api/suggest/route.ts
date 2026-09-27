import type { NextRequest } from "next/server";
import { getFromEngine } from "@/services/engine/backend";
import { webSuggestions } from "@/services/web-search";

const LIMIT = 8;
const FROM_INDEX_FIRST = 5;

/**
 * Suggests searches while the user types: completions from the KWIC index first, then
 * page titles from the online source (only when searching the web index).
 */
export async function GET(request: NextRequest) {
  const typed = request.nextUrl.searchParams.get("q") ?? "";
  const index = request.nextUrl.searchParams.get("index") ?? "";
  if (typed.trim() === "") return Response.json({ suggestions: [] });

  const [fromIndex, fromWeb] = await Promise.all([
    getFromEngine<{ suggestions: string[] }>("/suggest", { q: typed, index, limit: String(LIMIT) }),
    index ? Promise.resolve([]) : webSuggestions(typed, LIMIT),
  ]);
  const local = fromIndex.ok ? fromIndex.data.suggestions : [];

  // Never suggest exactly what the user typed.
  const seen = new Set<string>([typed.trim().toLowerCase()]);
  const suggestions: string[] = [];
  const add = (s: string) => {
    const key = s.trim().toLowerCase();
    if (key && !seen.has(key) && suggestions.length < LIMIT) {
      seen.add(key);
      suggestions.push(key);
    }
  };
  local.slice(0, FROM_INDEX_FIRST).forEach(add);
  fromWeb.forEach(add);
  local.slice(FROM_INDEX_FIRST).forEach(add);

  return Response.json({ suggestions });
}
