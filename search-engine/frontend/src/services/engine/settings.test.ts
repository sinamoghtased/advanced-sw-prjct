import { describe, expect, it } from "vitest";
import {
  DEFAULT_SETTINGS,
  addSettingsToParams,
  engineParams,
  settingsFromParams,
} from "./settings";

const read = (query: string) => settingsFromParams(new URLSearchParams(query));

describe("settingsFromParams", () => {
  it("uses the defaults when the address has no settings", () => {
    expect(read("")).toEqual(DEFAULT_SETTINGS);
    expect(DEFAULT_SETTINGS).toMatchObject({ mode: "or", caseSensitive: false, order: "relevance", size: 10 });
  });

  it("reads every setting", () => {
    expect(read("mode=and&case=true&symbols=-%26&order=clicks&size=20")).toEqual({
      mode: "and",
      caseSensitive: true,
      symbols: "-&",
      order: "clicks",
      size: 20,
    });
  });

  it("reads mode and order in any case", () => {
    expect(read("mode=NOT&order=Alphabetical")).toMatchObject({ mode: "not", order: "alphabetical" });
  });

  it("ignores values that are not allowed", () => {
    expect(read("mode=xor&order=newest&size=7&case=yes")).toEqual(DEFAULT_SETTINGS);
    expect(read("size=abc")).toEqual(DEFAULT_SETTINGS);
  });

  it("drops whitespace from the symbols and keeps them short", () => {
    expect(read("symbols=%20-%20%26%20").symbols).toBe("-&");
    expect(read(`symbols=${"x".repeat(80)}`).symbols).toHaveLength(50);
  });
});

describe("addSettingsToParams", () => {
  it("adds nothing for the default settings", () => {
    const params = new URLSearchParams();
    addSettingsToParams(params, DEFAULT_SETTINGS);
    expect(params.toString()).toBe("");
  });

  it("adds only the settings that differ from the defaults", () => {
    const params = new URLSearchParams();
    addSettingsToParams(params, { ...DEFAULT_SETTINGS, mode: "not", size: 50 });
    expect(params.toString()).toBe("mode=not&size=50");
  });

  it("round-trips through an address", () => {
    const settings = { mode: "and", caseSensitive: true, symbols: "-&", order: "bid", size: 5 } as const;
    const params = new URLSearchParams();
    addSettingsToParams(params, settings);
    expect(settingsFromParams(params)).toEqual(settings);
  });
});

describe("engineParams", () => {
  it("names the engine's search parameters", () => {
    expect(engineParams({ mode: "and", caseSensitive: true, symbols: "-", order: "bid", size: 5 })).toEqual({
      mode: "and",
      case: "true",
      symbols: "-",
      order: "bid",
    });
  });

  it("leaves case empty when it is off, so it is not sent", () => {
    expect(engineParams(DEFAULT_SETTINGS).case).toBe("");
  });
});
