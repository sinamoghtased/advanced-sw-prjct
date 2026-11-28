import type { NextRequest } from "next/server";
import { forwardToEngine } from "@/services/engine/backend";

/**
 * Passes the engine's change stream (server-sent events) on to the browser, so open pages
 * hear about added or deleted pages, clicks and bids. The stream ends when the browser leaves.
 */
export async function GET(request: NextRequest) {
  return forwardToEngine("/events", request.nextUrl.searchParams, [], { signal: request.signal });
}
