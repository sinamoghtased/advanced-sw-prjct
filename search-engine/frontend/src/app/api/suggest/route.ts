import type { NextRequest } from "next/server";
import { getFromEngine } from "@/services/engine/backend";

const LIMIT = 8;

/**
 * Suggests searches while the user types, using completions from the KWIC index.
 */
export async function GET(request: NextRequest) {
  const typed = request.nextUrl.searchParams.get("q") ?? "";
  const index = request.nextUrl.searchParams.get("index") ?? "";
  if (typed.trim() === "") return Response.json({ suggestions: [] });

  const fromIndex = await getFromEngine<{ suggestions: string[] }>("/suggest", {
    q: typed,
    index,
    limit: String(LIMIT),
  });
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
  local.forEach(add);

  return Response.json({ suggestions });
}
