import type { NextRequest } from "next/server";
import { forwardToEngine } from "@/services/engine/backend";

/** Builds an index from the posted text, streaming an update after every line. */
export async function POST(request: NextRequest) {
  return forwardToEngine("/index", request.nextUrl.searchParams, ["delay"], {
    method: "POST",
    headers: { "content-type": "text/plain; charset=utf-8" },
    body: await request.text(),
  });
}
