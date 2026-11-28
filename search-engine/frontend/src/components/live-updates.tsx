"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";

/** Changes that arrive together (a batch of pages, or several clicks) refresh the page once. */
export const REFRESH_DELAY_MS = 300;

/**
 * The view's side of the Observer pattern: listens to the engine's change stream and
 * refreshes the page's data when the engine says something changed, with no reload.
 * The browser's EventSource reconnects on its own if the stream drops.
 */
export default function LiveUpdates() {
  const router = useRouter();

  useEffect(() => {
    if (typeof EventSource === "undefined") return;
    const stream = new EventSource("/api/events");
    let timer: ReturnType<typeof setTimeout> | undefined;
    stream.onmessage = () => {
      clearTimeout(timer);
      timer = setTimeout(() => router.refresh(), REFRESH_DELAY_MS);
    };
    return () => {
      clearTimeout(timer);
      stream.close();
    };
  }, [router]);

  return null;
}
