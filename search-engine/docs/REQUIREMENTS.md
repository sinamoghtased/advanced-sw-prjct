# Requirements baseline — KWIC Index System

**CS/SE 6362 — Advanced Software Architecture and Design · Fall 2026**

This is the team's working requirements register: every requirement identifier in one place,
with its statement and its verification method, for use in planning, review, and ticking off
work as it lands.

> **Authority.** The [Software Requirements Specification](SRS.md) is the authoritative
> document. Where this register and the SRS disagree, the SRS governs. This file exists to be
> scanned; the SRS exists to be cited. Identifiers are identical in both, so a change to a
> requirement is made in the SRS first and reflected here.

**Verification methods:** **T** = Test (automated) · **D** = Demonstration (observed
execution) · **I** = Inspection (review of code or documents) · **A** = Analysis (reasoning or
measurement).

**Requirement language:** **shall** = binding · *should* = desirable, not binding ·
*may* = permitted option.

---

## Contents

- [Interpretation rulings](#interpretation-rulings)
- [Functional requirements](#functional-requirements)
- [Non-functional requirements](#non-functional-requirements)
- [External interfaces](#external-interfaces)
- [Constraints](#constraints)
- [Phase II apportionments](#phase-ii-apportionments)
- [Open issues](#open-issues)
- [Component allocation](#component-allocation)

---

## Interpretation rulings

The functional requirement supplied with the assignment is incomplete by design. These ten
rulings close the gaps. Each is a decision, not a deduction. Full discussion in
[SRS §4.1](SRS.md#41-interpretation-of-the-given-requirement-statement); worked demonstration
in [SRS Appendix A](SRS.md#appendix-a--worked-example).

| # | Question left open | Ruling | Realized by |
| - | ------------------ | ------ | ----------- |
| 1 | Where does input come from? | Direct text entry, file upload, built-in sample, plus a programmatic channel. | FR-1 – FR-4 |
| 2 | Where does output go? | Browser table, `.txt` and `.csv` export, JSON response. | FR-20 – FR-25 |
| 3 | Is the unshifted line itself a shift? | Yes. *n* words → *n* shifts, including degree 0. | FR-10, FR-11 |
| 4 | What does "alphabetical" mean for mixed case? | Case-insensitive by default, user-selectable; ties break by ordinal then degree. | FR-15 – FR-17 |
| 5 | Are duplicates collapsed? | No. Distinguished by source ordinal. | FR-13, FR-16 |
| 6 | Per line or corpus-wide ordering? | Corpus-wide. | FR-19 |
| 7 | What separates words? | Any run of whitespace. Punctuation stays with the word. | FR-6, FR-8 |
| 8 | Is the corpus bounded? | Yes, configurably. Oversized input rejected, never truncated. | FR-5 |
| 9 | *Extension:* noise words | Suppresses shifts whose **keyword** is a noise word. Off by default. | FR-14 |
| 10 | *Extension:* retrieval | Keyword-prefix filter — first increment toward Phase II. | FR-27 |

---

## Functional requirements

### A · Input acquisition

| ID | Requirement | V |
| -- | ----------- | - |
| **FR-1** | Accept a corpus entered directly in a multi-line text field, one line per newline. | D |
| **FR-2** | Accept a corpus uploaded as a UTF-8 plain-text file, accepting both LF and CRLF terminators. | T |
| **FR-3** | Provide at least one built-in sample corpus, loadable in a single action. | D |
| **FR-4** | Expose the indexing capability through a programmatic interface. | T |
| **FR-5** | Enforce a configurable corpus bound (default 10,000 lines / 1 MB) and reject excess with an explanation rather than truncating. | T |

### B · Validation and normalization

| ID | Requirement | V |
| -- | ----------- | - |
| **FR-6** | Treat any maximal run of whitespace as one separator; trim each line's edges. | T |
| **FR-7** | Ignore blank and whitespace-only lines; they contribute no shifts. | T |
| **FR-8** | Preserve original capitalization and punctuation in all output; normalization affects ordering only. | T |
| **FR-9** | Record each accepted line's source ordinal so every output entry is traceable to its line. | T |

### C · Circular shifting

| ID | Requirement | V |
| -- | ----------- | - |
| **FR-10** | Generate exactly *n* shifts for a line of *n* words; shift of degree *k* moves the first *k* words to the end, in order. | T |
| **FR-11** | Include the unshifted line (degree 0) among a line's shifts. | T |
| **FR-12** | Leave the stored corpus unaltered; it stays available for re-indexing under different options. | T |
| **FR-13** | Record each shift's source ordinal and degree; designate its first word as the keyword. | T |
| **FR-14** | With noise-word filtering enabled, omit shifts whose **keyword** is a noise word, retaining the line's other shifts. | T |

### D · Alphabetization

| ID | Requirement | V |
| -- | ----------- | - |
| **FR-15** | Order all shifts ascending, comparing word by word and, within a word, character by character. | T |
| **FR-16** | Order case-insensitively by default; break exact ties by source ordinal then degree, so output is byte-identical across runs. | T |
| **FR-17** | Allow the user to select case-sensitive collation before indexing. | D |
| **FR-18** | Where one shift is a proper prefix of another, order the shorter first. | T |
| **FR-19** | Order globally across the whole corpus, not within each line. | T |

### E · Output and presentation

| ID | Requirement | V |
| -- | ----------- | - |
| **FR-20** | Display the index as a scrollable, paginated table of keyword, context, and source ordinal. | D |
| **FR-21** | Visually distinguish keyword from context in every entry. | I |
| **FR-22** | Report lines accepted, lines ignored, and total shifts produced. | D |
| **FR-23** | Offer download of the complete index as plain text and as CSV. | D |
| **FR-24** | Return the ordered index as JSON from the programmatic interface, with the same fields as the CSV export. | T |
| **FR-25** | Paginate at a configurable page size (default 100); never render a whole larger index at once. | A |

### F · Index inspection and retrieval

| ID | Requirement | V |
| -- | ----------- | - |
| **FR-26** | Present the index in a form inspectable during demonstration *(satisfies CON-4)*. | D |
| **FR-27** | Filter the displayed index by keyword prefix. | T |
| **FR-28** | Retain the most recent index for the session, so filtering and pagination need no re-indexing. | T |

### G · Error handling

| ID | Requirement | V |
| -- | ----------- | - |
| **FR-29** | On empty input, do not index; state that at least one non-blank line is required. | T |
| **FR-30** | On undecodable or unaccepted input, reject with cause **and** remedy, leaving any existing index intact and visible. | T |
| **FR-31** | Report every rejection in the interface; no silent failure, no raw exception text. | T |

---

## Non-functional requirements

Each of the eight qualities named in the assignment is a word, not a requirement — none can be
passed or failed as given. The SRS refines each in three steps (*as stated → what is ambiguous
→ our ruling*) before decomposing it; that reasoning is the graded part and lives in
[SRS §5](SRS.md#5-non-functional-requirements). The thresholds alone are collected here.

### NFR-1 Understandability

*Ruling: governs development artifacts as seen by a maintaining developer new to the project.
End-user understandability is NFR-6.*

| ID | Criterion | V |
| -- | --------- | - |
| **NFR-1.1** | Every public class, method, and interface documents purpose, parameters, return value, error conditions. | I |
| **NFR-1.2** | A new developer locates the module implementing any FR within 5 minutes using the traceability matrix alone. | D |
| **NFR-1.3** | No method exceeds 40 lines or cyclomatic complexity 10 without justification in review. | A |
| **NFR-1.4** | Module names correspond one-to-one with architectural components; one naming convention throughout. | I |

### NFR-2 Portability

*Ruling: five distinct claims — OS, browser, runtime, deployment target, encoding — each stated
separately so none is claimed on another's strength.*

| ID | Criterion | V |
| -- | --------- | - |
| **NFR-2.1** | Core logic depends only on the standard library — no OS, browser, framework, or file-system API. | I |
| **NFR-2.2** | Builds and passes the full suite on macOS, Windows, and Linux under Node.js ≥ 20, identical commands. | T |
| **NFR-2.3** | UI fully functional on current and prior major Chrome, Firefox, Safari, Edge. | D |
| **NFR-2.4** | No source change between deployment targets; differences confined to configuration. | D |
| **NFR-2.5** | UTF-8 throughout; platform-independent line-terminator handling. | T |

### NFR-3 Enhanceability

*Ruling: modifiability is meaningless in the abstract — only with respect to a stated change
set. Anticipated changes: (a) add an output format · (b) change collation · (c) change the shift
definition · (d) add an input channel · (e) alter noise-word filtering · (f) replace the
in-memory index with a persistent store.*

| ID | Criterion | V |
| -- | --------- | - |
| **NFR-3.1** | Each change (a)–(f) is one new class implementing an existing interface plus one registration entry — no existing interface modified. | A + D |
| **NFR-3.2** | No module depends on another's internal data representation; all communication through published interfaces. | I |
| **NFR-3.3** | Effort estimated per change in the architecture spec; at least one implemented in Phase I to validate the estimate. | D |

### NFR-4 Reusability

*Ruling: the unit of reuse is the core library (line storage, shifter, alphabetizer); the target
context is any client that is not this web application.*

| ID | Criterion | V |
| -- | --------- | - |
| **NFR-4.1** | Core library has no dependency on HTTP, the DOM, the UI framework, or the file system. | I |
| **NFR-4.2** | Core library runs unmodified from a non-web client — unit tests and a CLI driver. | T |
| **NFR-4.3** | Every public core operation is a pure function of its inputs; no global or ambient state. | I + T |
| **NFR-4.4** | Public interface documented well enough to use without reading the implementation. | I |

### NFR-5 Performance

*Ruling: "good" is unmeasurable. **C_ref** = 1,000 lines × ~10 words ≈ 10,000 shifts, on
reference hardware recorded in the Test Plan. Thresholds provisional — see TBD-1, TBD-2.*

| ID | Criterion | V |
| -- | --------- | - |
| **NFR-5.1** | Index C_ref within 2 s at p95 over 20 runs. | T |
| **NFR-5.2** | Index a 100-line corpus within 200 ms. | T |
| **NFR-5.3** | Shift generation O(*N*), ordering O(*N* log *N*); measured *N* vs 4*N* within ±25% of prediction. | A + T |
| **NFR-5.4** | Peak memory ≤ 20× input size. | A |
| **NFR-5.5** | Prefix filter over a materialized index returns within 100 ms at C_ref. | T |
| **NFR-5.6** | No synchronous operation blocks the UI for more than 100 ms. | T |

### NFR-6 Usability

*Ruling: primary user is a first-time visitor with no training and no documentation, performing
the task "produce an alphabetized index from text I supply".*

| ID | Criterion | V |
| -- | --------- | - |
| **NFR-6.1** | First-time user completes the primary task in ≤ 3 interactions, without documentation. | D |
| **NFR-6.2** | Purpose of the system and of every control evident from on-screen labels alone. | I |
| **NFR-6.3** | Every error states what happened, why, and what to do next; no stack trace or internal identifier. | I |
| **NFR-6.4** | WCAG 2.1 AA for contrast, focus visibility, labeling; every function keyboard-operable. | T + D |
| **NFR-6.5** | User can correct input and re-index without losing text already entered. | D |
| **NFR-6.6** | ≥ 3 people outside the team complete the primary task unaided; times and errors recorded. | D |

### NFR-7 Responsiveness

*Ruling: the term conflates two unrelated meanings — temporal and layout. Both required, stated
separately.*

| ID | Criterion | V |
| -- | --------- | - |
| **NFR-7.1** | *Temporal* — every user action produces visible feedback within 100 ms. | T |
| **NFR-7.2** | *Temporal* — any operation over 500 ms shows a progress indicator and does not block interaction. | D |
| **NFR-7.3** | *Layout* — usable without horizontal scrolling from 320 px to 2560 px. | T |
| **NFR-7.4** | *Layout* — no overlap or clipping at any supported width; entries wrap rather than truncate. | D |
| **NFR-7.5** | *Layout* — legible under both light and dark color schemes. | D |

### NFR-8 Adaptability

*Ruling: separated from NFR-3 by who acts and at what cost. Enhanceability = a developer
modifies source. Adaptability = a deployer or user changes configuration, no rebuild.*

| ID | Criterion | V |
| -- | --------- | - |
| **NFR-8.1** | Noise-word list, page size, collation default, and corpus bounds configurable without editing source. | D |
| **NFR-8.2** | Configuration change needs no rebuild; documented defaults apply when configuration is absent. | T |
| **NFR-8.3** | Indexes any UTF-8 script without code change; language-specific collation selectable, not hard-coded. | T |
| **NFR-8.4** | Index store selectable by configuration, so the Phase II store replaces the in-memory one without altering calling code. | A |

### NFR-9 Robustness · *team-added*

*Implied by CON-4 — a system that crashes cannot be demonstrated — but never stated.*

| ID | Criterion | V |
| -- | --------- | - |
| **NFR-9.1** | No input within declared bounds causes abnormal termination. | T |
| **NFR-9.2** | Boundary suite covers: one-word line · single-character words · punctuation-only lines · repeated identical words · corpus at exactly the size limit · non-Latin script. | T |
| **NFR-9.3** | After any rejected request, the system remains fully operable without reload. | D |

### NFR-10 Testability · *team-added*

*Every requirement here names a verification method; that is only credible if the system is
built so verification can be performed.*

| ID | Criterion | V |
| -- | --------- | - |
| **NFR-10.1** | Every FR covered by ≥ 1 automated test citing its identifier. | I |
| **NFR-10.2** | Core library statement coverage ≥ 85%. | T |
| **NFR-10.3** | All tests deterministic and repeatable, relying on FR-16. | T |

### NFR-11 Availability and deployability · *team-added*

*CON-2 requires a reachable URL; "reachable" is empty without saying how reliably.*

| ID | Criterion | V |
| -- | --------- | - |
| **NFR-11.1** | Published URL reachable ≥ 99% of the grading window. | A |
| **NFR-11.2** | A merge to `main` deploys automatically, no manual step. | D |
| **NFR-11.3** | Deployed build traceable to the exact commit it was built from. | I |

### NFR-12 Safety of user-supplied content · *team-added*

*The system's defining act is rendering untrusted text back into a page — the precise shape of
an injection defect, forbidden by no other requirement here.*

| ID | Criterion | V |
| -- | --------- | - |
| **NFR-12.1** | User text always rendered as text, never markup or code — including CSV, where a leading `=`, `+`, `-`, or `@` must not be interpretable as a formula. | T |
| **NFR-12.2** | Uploads bounded in size and declared type *before* being read. | T |
| **NFR-12.3** | User text not persisted beyond the session without explicit user action. | I |

---

## External interfaces

| ID | Requirement | V |
| -- | ----------- | - |
| **UI-1** | Primary task on a single screen, no navigation required. | D |
| **UI-2** | That screen carries: text input, file upload, sample loader, collation and noise-word options, one control to index. | I |
| **UI-3** | Index shown as three distinct columns — keyword, context, ordinal — keyword column aligned for vertical scanning. | D |
| **UI-4** | Corpus statistics (FR-22) adjacent to the index, not on a separate screen. | I |
| **UI-5** | Progress indicated at all times while indexing runs. | D |
| **UI-6** | Errors presented next to the control that caused them, never as a modal interruption. | I |
| **API-1** | `POST` endpoint accepting a JSON corpus array plus an optional options object. | T |
| **API-2** | Responds with the ordered index — keyword, full shift, source ordinal — plus FR-22 statistics. | T |
| **API-3** | HTTP status distinguishes success, malformed request, and bounds exceeded; failures carry a machine-readable code and a human-readable message. | T |
| **API-4** | Schemas documented, versioned, backward-compatible within a major version. | I |
| **API-5** | Same bounds, defaults, and semantics as the UI, so the two cannot diverge. | T |

---

## Constraints

| ID | Constraint |
| -- | ---------- |
| **CON-1** | Object-oriented architectural style, justified against ≥ 1 alternative. |
| **CON-2** | Reachable through the team's public web page; URL in the user manual. |
| **CON-3** | Declared stack: Next.js · Node.js · GitHub · Vercel. |
| **CON-4** | Prototype demonstrable live, with index table contents visible during the presentation. |
| **CON-5** | Deliverables available online and offline, extended IEEE-style format. |
| **CON-6** | No requirement satisfied by a mechanism undemonstrable within the Phase I schedule. |

---

## Phase II apportionments

Out of scope for Phase I. Recorded because the architecture must accommodate them without
structural change (NFR-3.1(f), NFR-8.4) — an unrecorded future requirement cannot be designed
for.

| ID | Deferred capability |
| -- | ------------------- |
| **PH2-1** | Persist the index so it survives a session and can be shared. |
| **PH2-2** | Rank retrieved entries by relevance rather than index order. |
| **PH2-3** | Acquire documents from external sources rather than direct submission. |
| **PH2-4** | Full-text query beyond the keyword-prefix filter of FR-27. |
| **PH2-5** | Index formats other than plain text. |

---

## Open issues

| ID | Issue | Owner | Needed by |
| -- | ----- | ----- | --------- |
| **TBD-1** | Reference hardware specification not yet recorded. | Team | Test Plan |
| **TBD-2** | NFR-5.1, 5.2, 5.5 thresholds are estimates; re-baseline against the first prototype. | Team | Interim Project I |
| **TBD-3** | Team role play lives in the updated Project Plan; cross-reference to be added. | Sina Moghtased | Interim Project I |
| **TBD-4** | Published URL for CON-2 not yet fixed. | Team | User Manual |
| **TBD-5** | Default noise-word list for FR-14 not yet chosen. | Team | Prototype |
| **TBD-6** | FR-17 case-sensitive ordering — code-point or locale-aware collation? SRS Appendix A assumes code-point. | Team | Prototype |

---

## Component allocation

Intended allocation only; the Architectural Specification carries the authoritative matrix and
the rationale. The *secret* column is the load-bearing one — each component is defined by the
decision it conceals, so that decision can change without disturbing the others. That is what
makes NFR-3.2 checkable rather than rhetorical.

| Requirements | Component | Responsibility | Secret it hides |
| ------------ | --------- | -------------- | --------------- |
| FR-1 – FR-5, UI-2, API-1 | `Input` | Acquire a corpus from any channel | How a corpus arrived |
| FR-6 – FR-9 | `LineStorage` | Hold lines, words, ordinals | How text is represented in memory |
| FR-10 – FR-14 | `CircularShifter` | Produce and describe shifts | Whether shifts are materialized or computed on demand |
| FR-15 – FR-19 | `Alphabetizer` | Impose a total, deterministic order | Which ordering algorithm is used |
| FR-20 – FR-25, UI-3, UI-4 | `Output` | Render and export the index | Which formats exist and how they are produced |
| FR-26 – FR-28, API-2 | `IndexTable` | Retain the index; answer inspection and prefix queries | Where the index is stored (PH2-1) |
| FR-29 – FR-31, UI-5, UI-6 | `MasterControl` | Sequence the pipeline; surface every failure | The order and coupling of the stages |
