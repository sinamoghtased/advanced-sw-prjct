import type { NextRequest } from "next/server";
import { forwardToEngine } from "@/services/engine/backend";

/** Deletes an out-of-date page. The admin key travels in the x-admin-key header. */
export async function DELETE(request: NextRequest) {
  return forwardToEngine("/pages", request.nextUrl.searchParams, ["index", "url"], {
    method: "DELETE",
    headers: { "x-admin-key": request.headers.get("x-admin-key") ?? "" },
  });
}
