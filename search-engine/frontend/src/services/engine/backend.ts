/**
 * Forwards requests from the web page to the Quickdex engine (the Java service).
 *
 * The engine's address is read from QUICKDEX_BACKEND_URL, and defaults to a backend running
 * locally (see "Run locally" in the README).
 */
const ENGINE_URL = process.env.QUICKDEX_BACKEND_URL ?? "http://localhost:8080";

const PASSED_HEADERS = ["content-type", "content-disposition"];

const OFFLINE_MESSAGE =
  "The Quickdex backend is not running. Start it as described under \"Run locally\" in the README, then reload this page.";

export type EngineReply<T> =
  | { ok: true; data: T }
  | { ok: false; status: number; error: string };

/** Posts text to the engine and returns its JSON reply, for server code. */
export async function postToEngine<T>(path: string, body: string): Promise<EngineReply<T>> {
  let response: Response;
  try {
    response = await fetch(new URL(path, ENGINE_URL), {
      method: "POST",
      headers: { "content-type": "text/plain; charset=utf-8" },
      body,
      cache: "no-store",
    });
  } catch {
    return { ok: false, status: 503, error: OFFLINE_MESSAGE };
  }
  const data = (await response.json().catch(() => ({}))) as T & { error?: string };
  return response.ok
    ? { ok: true, data }
    : { ok: false, status: response.status, error: data.error ?? `HTTP ${response.status}` };
}

/** Fetches JSON from the engine, for server components. */
export async function getFromEngine<T>(
  path: string,
  params: Record<string, string>,
): Promise<EngineReply<T>> {
  const url = new URL(path, ENGINE_URL);
  for (const [name, value] of Object.entries(params)) {
    if (value !== "") url.searchParams.set(name, value);
  }
  let response: Response;
  try {
    response = await fetch(url, { cache: "no-store" });
  } catch {
    return { ok: false, status: 503, error: OFFLINE_MESSAGE };
  }
  if (!response.ok) {
    let error = `The search engine could not answer (HTTP ${response.status}). Try again.`;
    try {
      error = ((await response.json()) as { error?: string }).error ?? error;
    } catch {
      // Keep the generic message.
    }
    return { ok: false, status: response.status, error };
  }
  return { ok: true, data: (await response.json()) as T };
}

export async function forwardToEngine(
  path: string,
  params: URLSearchParams,
  allowed: string[],
  init?: RequestInit,
): Promise<Response> {
  const url = new URL(path, ENGINE_URL);
  for (const name of allowed) {
    const value = params.get(name);
    if (value !== null) url.searchParams.set(name, value);
  }

  let upstream: Response;
  try {
    upstream = await fetch(url, { ...init, cache: "no-store" });
  } catch {
    return Response.json({ error: OFFLINE_MESSAGE }, { status: 503 });
  }

  const headers = new Headers({ "cache-control": "no-cache, no-transform" });
  for (const name of PASSED_HEADERS) {
    const value = upstream.headers.get(name);
    if (value) headers.set(name, value);
  }
  // Pass the body through as a stream, so index updates reach the page as they happen.
  return new Response(upstream.body, { status: upstream.status, headers });
}
