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

### Structure: model, view and controller

The system has two layers, and together they follow model-view-controller:

- **Model:** the Java engine's data: the stored pages, the KWIC index, and the click and bid counts.
- **Controller:** the Java engine's `MasterControl` and HTTP server, which take requests, change the model and answer.
- **View:** this app's pages and components. The routes under `app/api/` and `services/engine`
  only forward requests to the engine (a Proxy); they hold no search logic.

The view follows the model by Observer. The engine tells its listeners (`ModelListener`) when
pages are added or deleted, or a click or bid changes. The server passes each change to the
browser as a server-sent event (`/events`, through `app/api/events`), and
`components/live-updates.tsx` refreshes the page's data when one arrives. A change made in one
tab shows up in the others without a reload.

Only the shared web index sends changes. An index built in the Indexer tab belongs to one page,
which already updates line by line as it is built.

## Deployment

The app deploys to Vercel from `main`, with the Vercel project's root directory set to
`search-engine/frontend`. Set `QUICKDEX_BACKEND_URL` in Vercel to a hosted backend.
