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

**Live site:** <https://advanced-sw-prjct.vercel.app> · **Repository:** <https://github.com/sinamoghtased/advanced-sw-prjct>

**Stack:** Next.js (frontend) · Java (backend) · GitHub (repository) · Vercel (frontend deployment).
The Java backend is not hosted yet, so searching on the live site needs a hosted backend; to try
the search engine now, [run it locally](#run-locally).

The home page is the search engine. It answers every search from its own KWIC index, built at
startup from [`backend/src/main/resources/corpus.txt`](backend/src/main/resources/corpus.txt),
with suggestions while you type. Live pages from free online sources (Wikipedia, Hacker News,
Stack Overflow, and arXiv) are optional and off by default; see the
[frontend README](frontend/README.md#online-sources) to opt in. The **Indexer** tab builds an
index from your own text, shows it updating after each line, and can open it in the search engine.

The project has two parts: a Java **backend** ([`backend/`](backend)), the KWIC engine and search
engine built with the ADT architecture in the design documents, and a Next.js **frontend**
([`frontend/`](frontend)), the web app.

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
| [Preliminary Project Plan](docs/SE6362-PreliminaryProjectPlan.pdf) | Deliverable schedule, team roles, and planned tools. The website serves a project-plan PDF at [`/project-plan.pdf`](frontend/public/project-plan.pdf). |

## Deliverable schedule

| Deliverable                     | Due        | Team leader    |
| ------------------------------- | ---------- | -------------- |
| Preliminary Project Plan        | 09/11/2026 | Sina Moghtased |
| Interim Project I (PPT + demo)  | 10/01/2026 | Sina Moghtased |
| Final Project I Submission      | 10/15/2026 | Sina Moghtased |
| Interim Project II              | 11/12/2026 | Sina Moghtased |
| Final Project II Submission     | 12/01/2026 | Sina Moghtased |

## Run locally

### 1. Install the tools

You need **Node.js 20 or newer**, **Java 21 or newer**, **Maven**, and **Git**. Check them with:

```bash
node -v     # v20 or newer
java -version   # 21 or newer
mvn -v
git --version
```

On macOS with Homebrew, install any that are missing with `brew install node openjdk maven git`.

### 2. Get the code

```bash
git clone https://github.com/sinamoghtased/advanced-sw-prjct.git
cd advanced-sw-prjct/search-engine
pwd
```

`pwd` prints the **project folder**, ending in `advanced-sw-prjct/search-engine`. The steps below
start from it. The backend and frontend are separate programs, each run from its own subfolder:
`backend/` and `frontend/`.

> If you already have the code, open a terminal and `cd` into the project folder first. Paths like
> `backend` only work from there; running `pwd` shows where you are.

### 3. Start the backend (terminal 1)

From the project folder:

```bash
cd backend && mvn package -DskipTests && java -jar target/quickdex-engine.jar
```

`mvn package` builds the backend into `target/quickdex-engine.jar`, and `java -jar` starts it. It is
ready when it prints `Quickdex engine listening on http://localhost:8080`. The first build can take
a minute while Maven downloads its plugins. Leave this terminal running.

### 4. Start the frontend (terminal 2)

Open a second terminal, `cd` into the project folder, then:

```bash
cd frontend && npm install && npm run dev
```

`npm install` is needed only the first time; after that, `cd frontend && npm run dev` is enough.

### 5. Open the app

| Page | Link |
| ---- | ---- |
| Search engine (home) | <http://localhost:3000> |
| Indexer: build your own index | <http://localhost:3000/indexer> |
| About | <http://localhost:3000/about> |
| Backend health check | <http://localhost:8080/health> |

To stop, press `Ctrl+C` in each terminal. After changing backend code, stop the backend and run
both backend commands again; the frontend reloads on its own.

### Troubleshooting

| Problem | Fix |
| ------- | --- |
| `cd: no such file or directory: backend` or `there is no POM in this directory` | The terminal is not in the project folder. Run `pwd`, `cd` into `advanced-sw-prjct/search-engine`, and try again |
| "The Quickdex backend is not running" on the page | Start the backend (step 3) and wait for its "listening" message, then reload the page |
| Port 3000 is in use | `npm run dev -- -p 3001`, then open <http://localhost:3001> |
| Port 8080 is in use | In `backend/`, start it with `java -jar target/quickdex-engine.jar 9090`; in `frontend/`, start with `QUICKDEX_BACKEND_URL=http://localhost:9090 npm run dev` |
| "Another next dev server is already running" | Stop the other one (`Ctrl+C` in its terminal) or open the address it prints |
| No live results from the web | Check your internet connection; searches still work on the pages already indexed |

For settings (ports, online sources, API keys) and every command, see the
[backend README](backend/README.md) and the [frontend README](frontend/README.md).

## Project structure

```
search-engine/
├── README.md       # This file: overview and how to run the project
├── backend/        # Java backend: the KWIC engine, search engine, and HTTP API (Maven)
│   └── README.md   # Build, run, test, HTTP API, and code layout
├── frontend/       # Next.js frontend: the web app (search engine, Indexer, About)
│   └── README.md   # Run, settings, online sources, and code layout
└── docs/           # Course deliverables: SRS, architecture, requirements, presentation, plan
```

## Deployment

The frontend deploys to Vercel from `main`, with the Vercel project's root directory set to
`search-engine/frontend`. The backend needs its own Java host, with `QUICKDEX_BACKEND_URL` set in
Vercel to point to it.
