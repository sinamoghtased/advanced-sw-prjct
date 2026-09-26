# Quickdex

Quickdex is an object-oriented Key Word In Context (KWIC) indexing system that generates circular word
shifts from input text and displays them in alphabetical order, forming the core of a
web-based search engine.

**CS/SE 6362 — Advanced Software Architecture and Design · Fall 2026 · UT Dallas**

| Team member      | Email                          |
| ---------------- | ------------------------------ |
| Yasin Sazid      | yasin.sazid@utdallas.edu       |
| Alessandro Botta | alessandro.botta@utdallas.edu  |
| Sina Moghtased   | sina.moghtased@utdallas.edu    |

**Planned stack:** Next.js (front end) · Java (back end) · GitHub (repository) · Vercel (front-end deployment). Java service hosting remains to be selected.

The current app is a landing page. The KWIC prototype is specified in `docs/INTERIM1.md`.

---

## Documentation

| Document | Contents |
| -------- | -------- |
| [**Interim Project I**](docs/INTERIM1.md) | Current requirements, ADT architecture, traceability, and trade-offs. Replaces the separate SRS and architecture documents. |
| [**Future requirements for later**](docs/FUTURE_REQUIREMENTS_FOR_LATER.md) | Deferred candidates, outside the Interim I baseline. |
| [Preliminary Project Plan](docs/SE6362-PreliminaryProjectPlan.pdf) | Deliverable schedule, team roles, and planned tools. The website serves a project-plan PDF at [`/project-plan.pdf`](public/project-plan.pdf). |

## Deliverable schedule

| Deliverable                     | Due        | Team leader    |
| ------------------------------- | ---------- | -------------- |
| Preliminary Project Plan        | 09/11/2026 | Sina Moghtased |
| Interim Project I (PPT + demo)  | 10/01/2026 | Sina Moghtased |
| Final Project I Submission      | 10/15/2026 | Sina Moghtased |
| Interim Project II              | 11/12/2026 | Sina Moghtased |
| Final Project II Submission     | 12/01/2026 | Sina Moghtased |

## Getting started

From the repository root:

```bash
cd search-engine
npm install
npm run dev
```

Open <http://localhost:3000>. The dev server hot-reloads on save; `Ctrl+C` stops it.
Use `npm run dev -- -p 3001` if port 3000 is taken.

| Command         | Purpose                             |
| --------------- | ----------------------------------- |
| `npm run dev`   | Local development server            |
| `npm run build` | Production build (type-checks too)  |
| `npm start`     | Serve the production build          |
| `npm run lint`  | ESLint                              |

Deployment is automatic from `main` via Vercel.

## Project structure

```
search-engine/
├── app/                    # Next.js App Router pages and layouts
│   ├── components/         # Shared UI components
│   ├── layout.tsx          # Root layout + navbar
│   └── page.tsx            # Landing page
├── docs/                   # Course deliverables
│   ├── INTERIM1.md         # Requirements and architecture for Interim I
│   ├── FUTURE_REQUIREMENTS_FOR_LATER.md
│   └── SE6362-PreliminaryProjectPlan.pdf
└── public/                 # Static assets served at the site root
```
