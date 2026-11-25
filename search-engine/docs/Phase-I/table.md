# Request flow: browser → Vercel → Render

Companion to [`diagram.md`](diagram.md): the same request flow as a table — which host runs
each step, and exactly what it calls.

## Flow A — Searching

| # | Component | Host | What happens |
| - | --------- | ---- | ------------ |
| 1 | `search-box.tsx` | Browser | Calls `GET /api/suggest` on every keystroke |
| 2 | `/api/suggest` route | Vercel | Calls `getFromEngine("/suggest", ...)` → HTTP to Render |
| 3 | `EngineServer` → `SearchEngine` | Render | Builds suggestions from the index |
| 4 | `search-box.tsx` | Browser | On Enter, calls `GET /api/search` instead |
| 5 | `/api/search` route | Vercel | Calls `getFromEngine("/search", ...)` → HTTP to Render |
| 6 | `SearchEngine.searchKeywordMatches()` | Render | Ranks matches, returns JSON |
| 7 | `page.tsx` | Browser | Renders the ranked results |

## Flow B — Building an index

| # | Component | Host | What happens |
| - | --------- | ---- | ------------ |
| 1 | `indexer-app.tsx` | Browser | Calls `POST /api/index` with the pasted text as the body |
| 2 | `/api/index` route | Vercel | `forwardToEngine()` pipes the response through as a stream |
| 3 | `QueueInputMedium` + `WebOutputMedium` + `MasterControl` | Render | `control.run()`: Input → CircularShift → Alphabetizer → Output, one line at a time |
| 4 | `WebOutputMedium` | Render | Streams one NDJSON line per update, Render → Vercel → browser |
| 5 | `indexer-app.tsx` | Browser | `ReadableStream` reader dispatches a state update per line — the live table fill |
| 6 | "Search these pages in the search engine →" | Browser | Re-runs Flow A, pointed at this index instead of the built-in one |

## Hosts

| Host | Runs | Notes |
| ---- | ---- | ----- |
| Browser | `page.tsx`, `search-box.tsx`, `indexer-app.tsx` | Never calls the Java backend directly |
| Vercel | Next.js, including every `app/api/*/route.ts` | Reads `QUICKDEX_BACKEND_URL` in `services/engine/backend.ts` to reach Render |
| Render | `EngineServer`, `SearchEngine`, `MasterControl` and the rest of the Java engine | Server-to-server only — never called directly by the browser |
