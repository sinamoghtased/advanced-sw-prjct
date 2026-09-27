/** Types and helpers for talking to the Quickdex engine from the browser. */

export type Entry = {
  id: number;
  line: number;
  keyword: string;
  context: string;
  url: string | null;
};

export type Placed = Entry & { pos: number };

/** An entry without its index position. */
export function toEntry({ id, line, keyword, context, url }: Placed): Entry {
  return { id, line, keyword, context, url };
}

export type Summary = {
  linesIndexed: number;
  linesSkipped: number;
  shifts: number;
  elapsedMillis: number;
};

export type EngineEvent =
  | { type: "start"; index: string; lines: number }
  | { type: "update"; line: number; size: number; inserted: Placed[] }
  | ({ type: "done" } & Summary)
  | { type: "error"; message: string };

/** One page found by a search, with the index entries that matched it. */
export type SearchResult = {
  line: number;
  url: string | null;
  text: string;
  phrase: boolean;
  missing: string[];
  matches: Placed[];
};

export type SearchResponse = {
  query: string;
  total: number;
  offset: number;
  elapsedMicros: number;
  pages: number;
  entries: number;
  results: SearchResult[];
};

/** Splits a search into lowercase words, the way the engine does. */
export function searchWords(query: string): string[] {
  return query.trim().toLowerCase().split(/\s+/u).filter(Boolean);
}

/** The engine's input limits (SRS FR7.0). */
export const MAX_LINES = 10_000;
export const MAX_BYTES = 1_000_000;
export const MAX_WORDS_PER_LINE = 50;

const URL_WORD = /^https?:\/\/\S+$/i;

/** A line's words, without the URL at its start or end (the engine's rule). */
export function wordsOf(line: string): string[] {
  const words = line.trim().split(/\s+/u).filter(Boolean);
  if (words.length > 0 && URL_WORD.test(words[0])) words.shift();
  else if (words.length > 0 && URL_WORD.test(words[words.length - 1])) words.pop();
  return words;
}

export function splitLines(text: string): string[] {
  const lines = text.split(/\r\n|\r|\n/);
  if (lines.length > 1 && lines[lines.length - 1] === "") lines.pop();
  return lines;
}

/** Checks the input before sending it; the engine checks it again. */
export function validate(text: string): string | null {
  if (text.trim() === "") return "The input is empty. Enter at least one line of text.";
  const bytes = new TextEncoder().encode(text).length;
  if (bytes > MAX_BYTES) {
    return `The input is ${bytes.toLocaleString("en-US")} bytes, over the 1 MB limit. Remove some text and try again.`;
  }
  const lines = splitLines(text);
  if (lines.length > MAX_LINES) {
    return `The input has ${lines.length.toLocaleString("en-US")} lines, over the ${MAX_LINES.toLocaleString("en-US")}-line limit. Remove some lines and try again.`;
  }
  for (let i = 0; i < lines.length; i++) {
    const count = wordsOf(lines[i]).length;
    if (count > MAX_WORDS_PER_LINE) {
      return `Line ${i + 1} has ${count} words, over the ${MAX_WORDS_PER_LINE}-word limit per line. Split it into shorter lines.`;
    }
  }
  if (lines.every((line) => wordsOf(line).length === 0)) {
    return "The input has no words, only URLs. Add some text to each line.";
  }
  return null;
}

/** Reads the engine's newline-delimited JSON stream, one batch of events per chunk. */
export async function* readEvents(
  body: ReadableStream<Uint8Array>,
): AsyncGenerator<EngineEvent[]> {
  const reader = body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  for (;;) {
    const { value, done } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split("\n");
    buffer = lines.pop() ?? "";
    const events = lines.filter((l) => l.trim() !== "").map((l) => JSON.parse(l) as EngineEvent);
    if (events.length > 0) yield events;
  }
  buffer += decoder.decode();
  if (buffer.trim() !== "") yield [JSON.parse(buffer) as EngineEvent];
}

/** Reads an error message from a failed response. */
export async function errorMessage(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as { error?: string };
    if (body.error) return body.error;
  } catch {
    // Not JSON; fall through.
  }
  return `Quickdex could not complete the request (HTTP ${response.status}). Try again.`;
}
