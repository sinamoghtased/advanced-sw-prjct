import type { NextRequest } from "next/server";
import { forwardToEngine } from "@/services/engine/backend";

/** Counts a click on a result's URL. */
export async function POST(request: NextRequest) {
  return forwardToEngine("/click", request.nextUrl.searchParams, ["index", "url"], { method: "POST" });
}
