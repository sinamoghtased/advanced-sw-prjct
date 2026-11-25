import { fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import ResultLink from "./result-link";

const fetchMock = vi.fn();

beforeEach(() => {
  fetchMock.mockResolvedValue(new Response("{}"));
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  fetchMock.mockReset();
  vi.unstubAllGlobals();
});

describe("ResultLink", () => {
  it("links to the result's web site in a new tab", () => {
    render(
      <ResultLink url="https://example.org/a" index="">
        Example
      </ResultLink>,
    );
    const link = screen.getByRole("link", { name: "Example" });
    expect(link).toHaveAttribute("href", "https://example.org/a");
    expect(link).toHaveAttribute("target", "_blank");
    expect(link).toHaveAttribute("rel", expect.stringContaining("noopener"));
  });

  it("counts a click on the built-in index", async () => {
    render(
      <ResultLink url="https://example.org/a?x=1&y=2" index="">
        Example
      </ResultLink>,
    );
    await userEvent.click(screen.getByRole("link"));

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [address, options] = fetchMock.mock.calls[0];
    expect(options).toMatchObject({ method: "POST", keepalive: true });
    const params = new URL(address, "http://localhost").searchParams;
    expect(new URL(address, "http://localhost").pathname).toBe("/api/click");
    expect(params.get("url")).toBe("https://example.org/a?x=1&y=2");
    expect(params.has("index")).toBe(false);
  });

  it("counts a click on one of the user's own indexes", async () => {
    render(
      <ResultLink url="https://example.org/a" index="abc123">
        Example
      </ResultLink>,
    );
    await userEvent.click(screen.getByRole("link"));
    const params = new URL(fetchMock.mock.calls[0][0], "http://localhost").searchParams;
    expect(params.get("index")).toBe("abc123");
  });

  it("counts a middle click but not other mouse buttons", () => {
    render(
      <ResultLink url="https://example.org/a" index="">
        Example
      </ResultLink>,
    );
    const link = screen.getByRole("link");
    fireEvent(link, new MouseEvent("auxclick", { bubbles: true, button: 2 }));
    expect(fetchMock).not.toHaveBeenCalled();
    fireEvent(link, new MouseEvent("auxclick", { bubbles: true, button: 1 }));
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("does not break when counting the click fails", async () => {
    fetchMock.mockRejectedValue(new Error("offline"));
    render(
      <ResultLink url="https://example.org/a" index="">
        Example
      </ResultLink>,
    );
    await userEvent.click(screen.getByRole("link"));
    expect(screen.getByRole("link")).toBeInTheDocument();
  });
});
