import type { NextRequest } from "next/server";
import { forwardToEngine } from "@/services/engine/backend";

/** Downloads an index as a CSV or plain-text file. */
export async function GET(request: NextRequest) {
  return forwardToEngine("/export", request.nextUrl.searchParams, ["index", "format"]);
}
