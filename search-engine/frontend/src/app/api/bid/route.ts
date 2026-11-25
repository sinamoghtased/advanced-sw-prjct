import type { NextRequest } from "next/server";
import { forwardToEngine } from "@/services/engine/backend";

/** Sets a page's bid for ordering results by bid. */
export async function POST(request: NextRequest) {
  return forwardToEngine("/bid", request.nextUrl.searchParams, ["index", "url", "amount"], {
    method: "POST",
    headers: { "x-admin-key": request.headers.get("x-admin-key") ?? "" },
  });
}
