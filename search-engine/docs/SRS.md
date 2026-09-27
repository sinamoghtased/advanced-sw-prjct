# Quickdex — Software Requirements Specification

> **Phase I** · CS/SE 6362 Advanced Software Architecture and Design · Fall 2026 · UT Dallas

| | |
| --- | --- |
| **Team** | Yasin Sazid · Alessandro Botta · Sina Moghtased |
| **Version** | 1.4 |
| **Date** | 27 September 2026 |
| **Aligned with** | Quickdex Architectural Design presentation ([Quickdex.pptx](Quickdex.pptx)) |

## Contents

1. [Overview](#1-overview)
2. [Functional Requirements](#2-functional-requirements)
3. [Non-Functional Requirements](#3-non-functional-requirements)
4. [Requirements Mapping](#4-requirements-mapping)
5. [Glossary](#5-glossary)
6. [Revision History](#6-revision-history)

---

## 1. Overview

### 1.1 What Quickdex does

Quickdex is a **Key Word In Context (KWIC)** indexing system. It takes lines of text, generates
every circular shift of every line, and lists all shifts in alphabetical order.

**Example:** the line *"Descent of Man"* produces three shifts:

| Keyword | Context |
| ------- | ------- |
| **Descent** | of Man |
| **of** | Man Descent |
| **Man** | Descent of |

### 1.2 How the requirements are organized

| Part | Covers | Verified through |
| ---- | ------ | ---------------- |
| **A · KWIC Generation** | The engine behind Quickdex: taking in lines, generating shifts, and sorting them. This is developer work. | Unit tests and the engine's logs |
| **B · Quickdex for Users** | How a person uses Quickdex through the web page. | The live web app |

### 1.3 Scope and constraints

- **In scope:** KWIC index generation, a web interface, and file export.
- **Out of scope (Phase I):** relevance ranking, fetching documents from the web, saving indexes
  between sessions, and user accounts.
- **Constraints:** Abstract Data Type (ADT) architecture · Next.js, Java, GitHub, Vercel ·
  public URL · a live demo that shows the index table.

### 1.4 Architecture at a glance

Quickdex uses the **Abstract Data Type (ADT)** style from Shaw and Garlan, redesigned with
descriptive operation names and a clear split between input, processing, and output. Each
component hides its data and algorithms and exposes only the operations other components need.

| Component | Responsibility | Public operations |
| --------- | -------------- | ----------------- |
| `MasterControl` | Calls the other components in order, once per line | `main()` |
| `Input` | Reads a line from the Input Medium and stores it | `setInput()` |
| `LineStorage` | Stores lines and hides how they are stored | `setCharacter()` · `getCharacter()` · `getWordCount()` |
| `CircularShift` | Generates the circular shifts of each line | `setLines()` · `getShiftedCharacter()` · `getShiftedWordCount()` |
| `Alphabetizer` | Keeps all shifts in global alphabetical order | `setShifts()` · `getAlphabetizedShift()` |
| `Output` | Retrieves the sorted shifts and shows them on the Output Medium | `getOutput()` |
| `SearchEngine` | Finds the index entries that match the user's keyword and shows them | `setSearchEngine()` |

The **Input Medium** and **Output Medium** are the web page. The diagrams, connections, and full
rationale are in [Architectural Design Decisions](Arch-design-decisions.md) and the architectural
design presentation ([Quickdex.pptx](Quickdex.pptx)).

---

## 2. Functional Requirements

### Part A · KWIC Generation

The KWIC engine builds the index **incrementally, one line at a time**. For each line,
`MasterControl.main()` has it read and stored, shifted, and merged into the sorted index, then
updates the output, before the next line is processed:

```
for each line:  read and store → generate circular shifts → merge into sorted index → update output → log
```

#### FR1.0 — Line Input

The KWIC engine shall accept a list of lines through a programmatic interface and process them
one at a time, in the order given. It shall ignore blank lines and extra spaces, and shall keep
every word exactly as it was entered.

#### FR2.0 — Circular Shifting

The KWIC engine shall generate every circular shift of the current line, including the original
line, so that a line of *n* words yields *n* shifts.

#### FR3.0 — Noise-Word Filtering (Phase II)

The KWIC engine shall, when enabled, remove shifts that begin with a noise word such as "the"
or "of".

#### FR4.0 — Incremental Alphabetical Sorting

The KWIC engine shall keep the already-sorted shifts of earlier lines and merge the current
line's shifts into them, so that the index is sorted after every line and earlier lines are never
re-processed or re-sorted. Sorting shall be case-insensitive by default or case-sensitive when selected, and
the same input shall always produce the same order.

#### FR5.0 — Generation Logging

The KWIC engine shall log its progress after each line, including:

- the line number and the line being processed, or that it was skipped
- the circular shifts generated for that line
- the size of the index after the shifts were inserted
- the time taken for that line

When all lines are processed, it shall log the final sorted index and the total time.

### Part B · Quickdex for Users

#### FR6.0 — Text Input

Quickdex shall allow the user to enter text in any of these ways:

- type or paste text into a text box
- upload a UTF-8 plain-text file
- load a built-in sample text

#### FR7.0 — Input Validation

Quickdex shall reject empty input, and input larger than 10,000 lines or 1 MB. The user shall
see a message that explains the problem and how to fix it.

#### FR8.0 — Index Options (Phase II)

Quickdex shall allow the user to choose case-sensitive or case-insensitive sorting, and to turn
noise-word filtering on or off, before creating the index.

#### FR9.0 — Index Display and Search

Quickdex shall display the index as a paginated table. Each row shall show the keyword, its
context, the source line number, and the URL associated with that entry. The table shall update
after each line is processed, keeping the entries from earlier lines.

The user shall be able to search the index by keyword. Quickdex shall then show only the entries
whose keyword starts with the search term.

#### FR10.0 — URL Navigation

Quickdex shall display each entry's URL as a clickable link that opens the corresponding page in
a new browser tab.

#### FR11.0 — Export

Quickdex shall allow the user to download the index as a plain-text or CSV file.

---

## 3. Non-Functional Requirements

### Part A · KWIC Generation

#### NFR1.0 — Understandability

The KWIC engine's code shall be documented well enough for a new developer to locate the code
for any FR within **5 minutes**. Every FR shall have at least one automated test.

#### NFR2.0 — Enhanceability

The KWIC engine shall allow a new input source, sorting rule, or output format to be added by
adding **a single class**, without changing existing interfaces.

#### NFR3.0 — Reusability

The KWIC engine shall not depend on the web page or the UI, so that other programs, such as a
command-line tool, can use it.

#### NFR4.0 — Performance

The KWIC engine shall index **1,000 lines** of about 10 words each in **under 2 seconds**.

#### NFR5.0 — Adaptability

The KWIC engine shall allow the noise-word list, default sort mode, and input limits to be
changed through configuration, without code changes or a rebuild.

#### NFR6.0 — Modifiability

The KWIC engine shall allow the internal implementation of any one component, such as how lines
are stored or which sorting algorithm is used, to be changed **without modifying any other
component**.

### Part B · Quickdex for Users

#### NFR7.0 — Portability

Quickdex shall work in the current and previous major versions of **Chrome, Firefox, Safari, and
Edge**, and its engine shall run on **macOS, Windows, and Linux**.

#### NFR8.0 — Usability

Quickdex shall let a first-time user create an index in **three steps** without instructions:
enter text, submit, and view the results.

#### NFR9.0 — Responsiveness

Quickdex shall:

- give visible feedback to every user action within **100 ms**
- filter results within **100 ms**
- fit screens from **320 px to 2560 px** wide without horizontal scrolling

#### NFR10.0 — Reliability

Quickdex shall not crash on any input within its limits, and shall keep working after an error.
It shall be reachable at its public URL at least **99%** of the time during grading.

#### NFR11.0 — Security

Quickdex shall always display user text as plain text and shall never execute it as code,
including in CSV exports.

---

## 4. Requirements Mapping

### 4.1 Functional requirements → components

Operation names follow the architecture presented in [Quickdex.pptx](Quickdex.pptx). Operations in
*italics* are private to their component.

| Part | Requirement | Component | Operations | Responsibility |
| :--: | ----------- | --------- | ---------- | -------------- |
| A | FR1.0 Line Input | `Input`, `LineStorage` | `setInput()`, *`storeInputInLineStorage()`* → `LineStorage.setCharacter()` | Reads each line from the Input Medium, cleans it, and stores it |
| A | FR2.0 Circular Shifting | `CircularShift` | `setLines()` → `LineStorage.getCharacter()`, `getWordCount()`; *`generateShifts()`* | Generates the shifts of the current line |
| A | FR3.0 Noise-Word Filtering | New Phase II component | — | Removes shifts that begin with a noise word, added without changing existing components |
| A | FR4.0 Incremental Alphabetical Sorting | `Alphabetizer` | `setShifts()` → `CircularShift.getShiftedCharacter()`, `getShiftedWordCount()`; *`alphabetize()`* | Merges each line's shifts into the retained sorted shifts |
| A | FR5.0 Generation Logging | `MasterControl` | `main()` | Feeds lines through the steps one at a time and logs each line |
| B | FR6.0 Text Input | Input Medium → `Input` | `setInput()` | Collects text from the user |
| B | FR7.0 Input Validation | `Input` | `setInput()` | Checks the input and reports errors to the user |
| B | FR8.0 Index Options | Input Medium → Phase II component, `Alphabetizer` | — | Passes the user's options to the engine |
| B | FR9.0 Index Display and Search | `Output`, `SearchEngine` | `Output.getOutput()` → `Alphabetizer.getAlphabetizedShift()`, *`displayOutput()`*; `SearchEngine.setSearchEngine()` → `Output.getOutput()`, *`searchKeywordMatches()`*, *`displayResults()`* | `Output` shows the index after every line; `SearchEngine` finds and shows the entries that match a keyword |
| B | FR10.0 URL Navigation | `Output`, `SearchEngine` | *`displayOutput()`*, *`displayResults()`* | Renders each entry's URL as a link that opens in a new tab |
| B | FR11.0 Export | `Output` | `getOutput()` | Exports the index as a file |

### 4.2 Non-functional requirements → design decisions

| Part | Requirement | Design decision |
| :--: | ----------- | --------------- |
| A | NFR1.0 Understandability | One focused responsibility per component, with descriptive operation names such as `getWordCount()` in place of vague ones such as *Word* |
| A | NFR2.0 Enhanceability | New features are added as new components (noise-word elimination in Phase II), and existing components are extended without exposing their internal processing |
| A | NFR3.0 Reusability | Each component has one focused job (`Input`: acquisition · `LineStorage`: storage · `CircularShift`: shift generation · `Alphabetizer`: global sorting · `Output`: presentation) and makes few assumptions about the others; the engine is a standalone Java library, separate from the web and UI layers |
| A | NFR4.0 Performance | Lines are processed incrementally in memory, and each line's shifts are merged into the already-sorted shifts (merge sort) instead of re-sorting everything. The accepted cost is memory: `Alphabetizer` keeps every shift, and components keep their own copies of data |
| A | NFR5.0 Adaptability | All tunable values are read from a configuration file |
| A | NFR6.0 Modifiability | **Algorithms** (shift generation, alphabetizing) are private to their components, and the **data representation** of lines, characters, and shifts is hidden behind public operations, so changing either stays inside one component |
| B | NFR7.0 Portability | Only standard web features; Java runs the same way on every OS |
| B | NFR8.0 Usability | Input, options, and results are on one screen, with errors shown next to their cause |
| B | NFR9.0 Responsiveness | The output updates after every line; a responsive layout and a paginated table keep each update small |
| B | NFR10.0 Reliability | Input size is capped, every error is caught and reported, and deploys are automatic from `main` |
| B | NFR11.0 Security | All user text is escaped on output, and CSV cells are protected from formula injection |

### 4.3 Architectural constraints → requirements

The four constraints of the architecture, and the requirements each one serves:

| Constraint | Rule | Serves |
| ---------- | ---- | ------ |
| **Incremental processing** | Lines are processed one at a time | FR1.0, FR4.0, FR5.0 |
| **Incremental merge** | Earlier alphabetized shifts are kept and merged with the current line's shifts | FR4.0, NFR4.0 |
| **Output timing** | The output keeps updating as each line is processed | FR9.0, NFR9.0 |
| **Information hiding** | Each component exposes only the operations other components need | NFR2.0, NFR3.0, NFR6.0 |

---

## 5. Glossary

| Term | Meaning |
| ---- | ------- |
| **Circular shift** | A line with some words moved from the front to the end. |
| **Keyword** | The first word of a shift. |
| **Context** | The rest of the shift after the keyword. |
| **Noise word** | A common word (such as *the* or *of*) that can be excluded from being a keyword. |
| **Abstract Data Type (ADT)** | An architectural style in which each component owns its data and exposes it only through public operations. |
| **Input Medium / Output Medium** | Where input comes from and where results go; in Quickdex, the web page. |
| **Incremental merge** | Merging a new line's sorted shifts into the shifts already sorted, instead of sorting everything again. |

---

## 6. Revision History

| Version | Date | Change |
| ------- | ---- | ------ |
| 1.0 | 22 Sep 2026 | Initial baseline |
| 1.1 | 22 Sep 2026 | Condensed to FRs, NFRs, and their mapping |
| 1.2 | 22 Sep 2026 | System named Quickdex; 10 FRs and 10 NFRs |
| 1.3 | 22 Sep 2026 | Requirements split into KWIC Generation and User parts; layout cleaned up; URL navigation added (11 FRs); Modifiability added (11 NFRs) |
| 1.4 | 27 Sep 2026 | Aligned with the architectural design presentation: architecture overview (§1.4); component and operation names; incremental merge (FR4.0); output updates after each line and a separate `SearchEngine` (FR9.0); design decisions follow the presented rationale; architectural constraints mapped to requirements (§4.3) |
