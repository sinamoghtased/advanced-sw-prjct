import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { DEFAULT_SETTINGS, type SearchSettings } from "@/services/engine/settings";
import SearchBox from "./search-box";

const router = vi.hoisted(() => ({ push: vi.fn(), refresh: vi.fn() }));
vi.mock("next/navigation", () => ({ useRouter: () => router }));

function show(query: string, settings: SearchSettings = DEFAULT_SETTINGS, index = "") {
  render(<SearchBox query={query} index={index} settings={settings} />);
}

/** The address the box last sent the browser to. */
function lastAddress() {
  const address = router.push.mock.lastCall?.[0] as string;
  const url = new URL(address, "http://localhost");
  return { path: url.pathname, params: url.searchParams };
}

beforeEach(() => {
  router.push.mockReset();
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(JSON.stringify({ suggestions: [] }))));
});

describe("search settings panel", () => {
  it("starts closed with the default settings", () => {
    show("java");
    expect(screen.getByText("Search settings").closest("details")).not.toHaveAttribute("open");
  });

  it("starts open when a setting is not the default", () => {
    show("java", { ...DEFAULT_SETTINGS, mode: "and" });
    expect(screen.getByText("Search settings").closest("details")).toHaveAttribute("open");
  });

  it("shows the current setting of each control", () => {
    show("java", { mode: "not", caseSensitive: true, symbols: "-&", order: "bid", size: 20 });
    expect(screen.getByLabelText("Search mode")).toHaveValue("not");
    expect(screen.getByLabelText("Order results by")).toHaveValue("bid");
    expect(screen.getByLabelText("Results per page")).toHaveValue("20");
    expect(screen.getByLabelText("Symbols to ignore")).toHaveValue("-&");
    expect(screen.getByLabelText("Match case")).toBeChecked();
  });

  it("runs the showing search again when the mode changes", async () => {
    show("java script");
    await userEvent.selectOptions(screen.getByLabelText("Search mode"), "and");
    const { path, params } = lastAddress();
    expect(path).toBe("/");
    expect(params.get("q")).toBe("java script");
    expect(params.get("mode")).toBe("and");
  });

  it("runs it again for the order, the page size and match case", async () => {
    show("java");
    await userEvent.selectOptions(screen.getByLabelText("Order results by"), "clicks");
    expect(lastAddress().params.get("order")).toBe("clicks");

    await userEvent.selectOptions(screen.getByLabelText("Results per page"), "50");
    const { params } = lastAddress();
    expect(params.get("size")).toBe("50");
    expect(params.get("order")).toBe("clicks");

    await userEvent.click(screen.getByLabelText("Match case"));
    expect(lastAddress().params.get("case")).toBe("true");
    expect(lastAddress().params.get("size")).toBe("50");
  });

  it("applies the symbols to ignore when the field is left, without spaces", async () => {
    show("selfdriving");
    await userEvent.type(screen.getByLabelText("Symbols to ignore"), "- &");
    expect(router.push).not.toHaveBeenCalled();
    await userEvent.tab();
    expect(lastAddress().params.get("symbols")).toBe("-&");
    expect(lastAddress().params.get("q")).toBe("selfdriving");
  });

  it("keeps the user's own index in the address", async () => {
    show("java", DEFAULT_SETTINGS, "abc123");
    await userEvent.selectOptions(screen.getByLabelText("Search mode"), "not");
    expect(lastAddress().params.get("index")).toBe("abc123");
  });

  it("drops a setting from the address when it goes back to the default", async () => {
    show("java", { ...DEFAULT_SETTINGS, mode: "and" });
    await userEvent.selectOptions(screen.getByLabelText("Search mode"), "or");
    expect(lastAddress().params.has("mode")).toBe(false);
  });

  it("does not search when there is no query yet, but the next search uses the settings", async () => {
    show("");
    await userEvent.selectOptions(screen.getByLabelText("Search mode"), "and");
    await userEvent.click(screen.getByLabelText("Match case"));
    expect(router.push).not.toHaveBeenCalled();

    await userEvent.type(screen.getByRole("combobox", { name: "Search" }), "java{Enter}");
    const { params } = lastAddress();
    expect(params.get("q")).toBe("java");
    expect(params.get("mode")).toBe("and");
    expect(params.get("case")).toBe("true");
  });
});
