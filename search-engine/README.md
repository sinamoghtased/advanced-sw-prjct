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

**Stack:** Next.js (front end) · Node.js (back end) · GitHub (repository) · Vercel (deployment)

---

## Documentation

| Document | Contents |
| -------- | -------- |
| [**Software Requirements Specification**](docs/SRS.md) | The Phase I requirements deliverable: 10 functional and 10 non-functional requirements, and their mapping to the architecture. **Authoritative.** |
| [**Architectural Design Decisions**](docs/Arch-design-decisions.md) | Object-oriented (ADT) architecture: components, connections, constraints, diagrams, and rationale based on the NFRs. |
| [**Requirements register**](docs/REQUIREMENTS.md) | A checklist of every requirement ID, with its component and verification method. |
| [Preliminary Project Plan](docs/SE6362-PPP.pdf) | Deliverable schedule and team roles. Also published at [`/project-plan.pdf`](public/project-plan.pdf) and linked from the site navigation. |

Still to come: Test Plan and User Manual.

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
│   ├── SRS.md              # Software Requirements Specification
│   ├── REQUIREMENTS.md     # Requirements register
│   └── SE6362-PPP.pdf      # Preliminary Project Plan
└── public/                 # Static assets served at the site root
```
