import { afterEach, describe, expect, it, vi } from "vitest";
import { GET } from "./route";

afterEach(() => vi.unstubAllGlobals());

function requestFor(signal?: AbortSignal) {
  return { nextUrl: new URL("http://localhost:3000/api/events"), signal } as Parameters<typeof GET>[0];
}

describe("GET /api/events", () => {
  it("passes the engine's event stream on to the browser", async () => {
    const stream = new Response("data: {}\n\n", { headers: { "content-type": "text/event-stream" } });
    const fetchMock = vi.fn().mockResolvedValue(stream);
    vi.stubGlobal("fetch", fetchMock);
    const controller = new AbortController();

    const response = await GET(requestFor(controller.signal));

    expect(String(fetchMock.mock.calls[0][0])).toMatch(/\/events$/);
    expect(fetchMock.mock.calls[0][1].signal).toBe(controller.signal);
    expect(response.headers.get("content-type")).toBe("text/event-stream");
    expect(response.headers.get("cache-control")).toContain("no-cache");
    expect(await response.text()).toBe("data: {}\n\n");
  });

  it("answers 503 when the engine is not running", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new Error("connection refused")));
    const response = await GET(requestFor());
    expect(response.status).toBe(503);
  });
});
