# Quickdex frontend

The Next.js web app: the search engine (home page), the Indexer, the Manual, and the About page.
It sends
searches to the [Java backend](../backend), which answers from its own KWIC index built from
[`corpus.txt`](../backend/src/main/resources/corpus.txt). Live results from free online sources
are optional and off by default (see [Online sources](#online-sources)).

## Requirements

Node.js 20 or newer, and a running [backend](../backend).

## Run

From this folder:

```bash
npm install   # first time only
npm run dev   # start the app on http://localhost:3000
```

| Command | Purpose |
| ------- | ------- |
| `npm run dev` | Development server, reloads on save |
| `npm run build` | Production build (type-checks too) |
| `npm start` | Serve the production build |
| `npm run lint` | Lint the code |

## Configuration

Copy [`.env.example`](.env.example) to `.env.local` to change these settings:

| Variable | Default | Purpose |
| -------- | ------- | ------- |
| `QUICKDEX_BACKEND_URL` | `http://localhost:8080` | Where the Java backend runs |
| `QUICKDEX_WEB_SOURCES` | `off` | Online sources for live results, separated by commas, or `off` (Phase I default: answer only from `corpus.txt`) |
| `STACKEXCHANGE_KEY` | — | Optional free key that raises Stack Exchange's limit from 300 to 10,000 requests a day |
| `BRAVE_API_KEY` | — | Adds [Brave Search](https://brave.com/search/api/) for results from the whole web (paid after a monthly credit) |

## Online sources

Phase I answers every search only from the backend's own index, built from
[`corpus.txt`](../backend/src/main/resources/corpus.txt) (or the file `QUICKDEX_CORPUS` points
to). Live web sources are optional: set `QUICKDEX_WEB_SOURCES` to opt back in. When enabled, the
app asks every listed source for matching pages and posts them to the backend, which adds new
ones to its KWIC index one line at a time and then answers the search. Answers are cached for a
day, and a source that fails or is paused is skipped.

| Source | Adds | Key | Limit |
| ------ | ---- | --- | ----- |
| Wikipedia | Encyclopedia articles | None | 200 requests a minute |
| Hacker News (Algolia) | Articles from across the web | None | 10,000 requests an hour |
| Stack Overflow (Stack Exchange) | Programming questions | Optional | 300 a day, or 10,000 with a key |
| arXiv | Research papers | None | One request every 3 seconds; searches in between skip arXiv |
| Brave Search | The whole web | Required | Monthly credit, then paid |

## Code layout

```
src/
├── app/                    # Pages and routes (Next.js App Router)
│   ├── page.tsx            # Search tab: the search engine (home page)
│   ├── indexer/            # Indexer tab: build an index and watch it update line by line
│   ├── manual/             # Manual tab: preliminary user manual
│   ├── about/              # About tab: project, architecture, and team
│   ├── api/                # Routes the browser calls: search, suggest, index, export
│   └── layout.tsx          # Page layout and navbar
├── components/             # Navbar, and the search box with suggestions
└── services/
    ├── engine/             # Calls to the Java backend (backend.ts) and shared types (client.ts)
    └── web-search/         # One file per online source, and index.ts to combine them
public/                     # Static files served at the site root
```

## Deployment

The app deploys to Vercel from `main`, with the Vercel project's root directory set to
`search-engine/frontend`. Set `QUICKDEX_BACKEND_URL` in Vercel to a hosted backend.
