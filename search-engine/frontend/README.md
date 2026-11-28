# Quickdex frontend

The Next.js web app: the search engine (home page), the Indexer, the Manual, and the About page.
It sends
searches to the [Java backend](../backend), which answers from its own KWIC index built from
[`corpus.txt`](../backend/src/main/resources/corpus.txt).

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
public/                     # Static files served at the site root
```

### Structure: two layers

The system has two layers. The web layer (this app) is the view: the pages and components draw
the screen and send what the user types. The routes under `app/api/` and `services/engine`
only forward requests to the Java engine and pass its answers back. They hold no search logic
and call no other service. The Java engine is the controller (`MasterControl`) around the
models (the stored pages, the index and the click and bid counts).

This is a loose model-view-controller split, not a full one. The view does not get updated
automatically when the model changes (no Observer): a page shows new data only after it asks
the engine again, for example by searching or reloading.

## Deployment

The app deploys to Vercel from `main`, with the Vercel project's root directory set to
`search-engine/frontend`. Set `QUICKDEX_BACKEND_URL` in Vercel to a hosted backend.
