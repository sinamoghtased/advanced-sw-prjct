import { act, render } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import LiveUpdates, { REFRESH_DELAY_MS } from "./live-updates";

const router = vi.hoisted(() => ({ refresh: vi.fn() }));
vi.mock("next/navigation", () => ({ useRouter: () => router }));

class FakeEventSource {
  static last: FakeEventSource;
  onmessage: (() => void) | null = null;
  closed = false;
  constructor(public url: string) {
    FakeEventSource.last = this;
  }
  close() {
    this.closed = true;
  }
}

beforeEach(() => {
  vi.useFakeTimers();
  vi.stubGlobal("EventSource", FakeEventSource);
  router.refresh.mockClear();
});

afterEach(() => {
  vi.useRealTimers();
  vi.unstubAllGlobals();
});

describe("LiveUpdates", () => {
  it("listens to the engine's change stream", () => {
    render(<LiveUpdates />);
    expect(FakeEventSource.last.url).toBe("/api/events");
  });

  it("refreshes the page when the engine reports a change", () => {
    render(<LiveUpdates />);
    FakeEventSource.last.onmessage?.();
    expect(router.refresh).not.toHaveBeenCalled();
    act(() => vi.advanceTimersByTime(REFRESH_DELAY_MS));
    expect(router.refresh).toHaveBeenCalledTimes(1);
  });

  it("refreshes once for changes that arrive together", () => {
    render(<LiveUpdates />);
    for (let i = 0; i < 5; i++) FakeEventSource.last.onmessage?.();
    act(() => vi.advanceTimersByTime(REFRESH_DELAY_MS));
    expect(router.refresh).toHaveBeenCalledTimes(1);
  });

  it("closes the stream and drops a pending refresh when the page is left", () => {
    const { unmount } = render(<LiveUpdates />);
    FakeEventSource.last.onmessage?.();
    unmount();
    act(() => vi.advanceTimersByTime(REFRESH_DELAY_MS));
    expect(FakeEventSource.last.closed).toBe(true);
    expect(router.refresh).not.toHaveBeenCalled();
  });
});
