# Quickdex — Folder Structure and Architecture

Companion diagrams to [`Arch-design-decisions.md`](Arch-design-decisions.md): where the code lives,
and how a search request flows through it.

## Folder structure

```mermaid
%%{init: {"themeVariables": {"fontSize": "17px"}, "flowchart": {"curve": "basis", "nodeSpacing": 26, "rankSpacing": 55, "padding": 10}}}%%
flowchart TB
    ROOT["search-engine/"]
    README["README.md<br/>overview · run locally · deployment"]

    ROOT --> README
    ROOT --> BACKEND
    ROOT --> FRONTEND
    ROOT --> DOCS

    subgraph BACKEND["backend/ — Java (Maven)"]
        direction TB
        B_POM["pom.xml"]
        B_MAIN["src/main/java/.../quickdex/<br/>MasterControl · Input · LineStorage<br/>CircularShift · Alphabetizer · Output<br/>SearchEngine · Mediums · records"]
        B_API["src/main/java/.../quickdex/api/<br/>EngineServer · WebOutputMedium · Json"]
        B_RES["src/main/resources/<br/>corpus.txt — seed dataset"]
        B_TEST["src/test/java/...<br/>JUnit 5 tests"]
        B_POM --- B_MAIN --- B_API --- B_RES --- B_TEST
    end

    subgraph FRONTEND["frontend/ — Next.js"]
        direction TB
        F_APP["src/app/<br/>page.tsx (Search) · indexer/ · about/ · api/"]
        F_COMP["src/components/<br/>navbar.tsx · search-box.tsx"]
        F_SVC_E["src/services/engine/<br/>backend.ts · client.ts"]
        F_SVC_W["src/services/web-search/<br/>wikipedia · hackernews · stackexchange<br/>arxiv · brave · index.ts"]
        F_APP --- F_COMP --- F_SVC_E --- F_SVC_W
    end

    subgraph DOCS["docs/"]
        direction TB
        D1["SRS.md · REQUIREMENTS.md"]
        D2["Arch-design-decisions.md"]
        D3["Quickdex.pptx · diagrams"]
        D1 --- D2 --- D3
    end

    classDef root fill:#e2e8f0,stroke:#475569,stroke-width:1.5px,color:#111
    classDef file fill:#f8fafc,stroke:#94a3b8,stroke-width:1px,color:#111
    class ROOT,README root
    class B_POM,B_MAIN,B_API,B_RES,B_TEST,F_APP,F_COMP,F_SVC_E,F_SVC_W,D1,D2,D3 file

    style BACKEND fill:#22c55e1a,stroke:#16a34a,stroke-width:1.5px
    style FRONTEND fill:#3b82f61a,stroke:#3b82f6,stroke-width:1.5px
    style DOCS fill:#a855f71a,stroke:#9333ea,stroke-width:1.5px
```

## Architecture (request flow)

```mermaid
%%{init: {"themeVariables": {"fontSize": "17px"}, "flowchart": {"curve": "basis", "nodeSpacing": 28, "rankSpacing": 55, "padding": 10}}}%%
flowchart TB
    Browser(["Browser"])

    subgraph FE["Frontend — Next.js"]
        direction TB
        SB["SearchBox<br/>suggestions dropdown"]
        PAGE["page.tsx<br/>Search — home page"]
        IDX["indexer-app.tsx<br/>Indexer tab"]
        SVCE["services/engine/backend.ts"]
        WS["services/web-search/*<br/>Wikipedia · HN · Stack Exchange · arXiv · Brave<br/>optional — off by default"]
    end

    subgraph BE["Backend — Java"]
        direction TB
        ES["EngineServer<br/>/health /stats /suggest /pages /index /search /export"]
        MC(["MasterControl"])

        subgraph PIPE["KWIC pipeline — one pass per line"]
            direction LR
            INm["Input"] --> LSm["LineStorage"] --> CSm["CircularShift"] --> ALm["Alphabetizer"] --> OUTm["Output"]
        end

        SE["SearchEngine<br/>match · rank · suggest"]
        CORPUS[("corpus.txt")]
    end

    Browser --> SB --> SVCE
    Browser --> PAGE --> SVCE
    Browser --> IDX --> SVCE
    PAGE -.-> WS
    WS -. "POST /pages" .-> ES

    SVCE == "HTTP :8080" ==> ES
    ES --> MC
    MC --> INm
    OUTm --> SE
    SE --> ES

    CORPUS -. "read at startup" .-> MC

    classDef comp fill:#e2e8f0,stroke:#475569,stroke-width:1.5px,color:#111
    classDef medium fill:#64748b,stroke:#334155,stroke-width:2px,color:#fff
    class ES,MC,INm,LSm,CSm,ALm,OUTm,SE comp
    class CORPUS medium

    style FE fill:#3b82f61a,stroke:#3b82f6,stroke-width:1.5px
    style BE fill:#22c55e1a,stroke:#16a34a,stroke-width:1.5px
    style PIPE fill:#f8fafc,stroke:#94a3b8,stroke-width:1px
    style WS fill:#fef3c71a,stroke:#d97706,stroke-width:1.5px,stroke-dasharray:4 3
```

**How to read it**

| Element | Meaning |
| ------- | ------- |
| Blue box | Frontend (Next.js) |
| Green box | Backend (Java) |
| Gray box | A component or pipeline stage |
| Dark hexagon | `corpus.txt`, the seed dataset read at startup |
| Dashed amber box | Optional live web sources — disabled by default in Phase I; `corpus.txt` alone answers searches |
| Thick arrow | The HTTP boundary between frontend and backend (`:8080`) |
| Dotted arrow | An optional or startup-only path |

Two index instances exist at runtime: the **web index**, seeded from `corpus.txt` and searched by
the home page, and any number of **user-built indexes** (Indexer tab, `POST /index`), kept in an
LRU cache. Both run through the identical `MasterControl` pipeline shown above — only the
`InputMedium`/`OutputMedium` differ, which is the information-hiding point of the ADT
architecture (see [`Arch-design-decisions.md`](Arch-design-decisions.md)).
