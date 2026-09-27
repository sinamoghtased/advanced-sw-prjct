import type { NextRequest } from "next/server";
import { forwardToEngine } from "@/services/engine/backend";

/** Returns the pages of an index that match a search. */
export async function GET(request: NextRequest) {
  return forwardToEngine("/search", request.nextUrl.searchParams, ["index", "q", "offset", "limit"]);
}
