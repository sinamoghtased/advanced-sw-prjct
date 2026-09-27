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
| [**Interim Project I**](docs/INTERIM1.md) | Current requirements, ADT architecture, traceability, and trade-offs. |
| [**Software Requirements Specification**](docs/SRS.md) | The Phase I requirements: 10 functional and 10 non-functional requirements, and their mapping to the architecture. |
| [**Architectural Design Decisions**](docs/Arch-design-decisions.md) | Object-oriented (ADT) architecture: components, connections, constraints, diagrams, and rationale based on the NFRs. |
| [**Requirements register**](docs/REQUIREMENTS.md) | A checklist of every requirement ID, with its component and verification method. |
| [**Future requirements for later**](docs/FUTURE_REQUIREMENTS_FOR_LATER.md) | Deferred candidates, outside the Interim I baseline. |
| [Implementation and test notes](docs/IMPLEMENTATION_AND_TEST_NOTES.md) | Implementation and test notes for the Interim I requirements, with proposed limits and timing targets. |
| [Architecture presentation](docs/Quickdex.pptx) | Slides for the architectural design. |
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
│   ├── SRS.md              # Software Requirements Specification
│   ├── Arch-design-decisions.md  # Architecture, diagrams, and rationale
│   ├── REQUIREMENTS.md     # Requirements register
│   ├── FUTURE_REQUIREMENTS_FOR_LATER.md
│   ├── IMPLEMENTATION_AND_TEST_NOTES.md
│   ├── Quickdex.pptx       # Architecture presentation
│   └── SE6362-PreliminaryProjectPlan.pdf
└── public/                 # Static assets served at the site root
```
