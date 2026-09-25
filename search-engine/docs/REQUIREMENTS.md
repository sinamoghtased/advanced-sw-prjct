# Requirements register — Quickdex

**CS/SE 6362 — Advanced Software Architecture and Design · Fall 2026**

A checklist of the requirements in the [SRS](SRS.md). If the two ever disagree, the SRS is
correct.

**Verification:** **T** Test · **D** Demonstration · **I** Inspection · **A** Analysis · **L** Logs

## Part A — KWIC Generation

| ID | Name | Component | V |
| -- | ---- | --------- | - |
| FR1.0 | Line Input | `Input`, `LineStorage` | T, L |
| FR2.0 | Circular Shifting | `CircularShifter` | T, L |
| FR3.0 | Noise-Word Filtering | `CircularShifter` | T, L |
| FR4.0 | Incremental Alphabetical Sorting | `Alphabetizer` | T, L |
| FR5.0 | Generation Logging | `MasterControl` | L |
| NFR1.0 | Understandability | — | I, D |
| NFR2.0 | Enhanceability | — | A, D |
| NFR3.0 | Reusability | — | I, T |
| NFR4.0 | Performance | — | T, L |
| NFR5.0 | Adaptability | — | D |
| NFR6.0 | Modifiability | — | A, D |

## Part B — Quickdex for Users

| ID | Name | Component | V |
| -- | ---- | --------- | - |
| FR6.0 | Text Input | Web UI, `Input` | D |
| FR7.0 | Input Validation | Web UI, `Input` | T |
| FR8.0 | Index Options | Web UI | D |
| FR9.0 | Index Display and Search | `Output`, `IndexTable` | D |
| FR10.0 | URL Navigation | `Output` | D |
| FR11.0 | Export | `Output` | T |
| NFR7.0 | Portability | — | T, D |
| NFR8.0 | Usability | — | D |
| NFR9.0 | Responsiveness | — | T, D |
| NFR10.0 | Reliability | — | T, A |
| NFR11.0 | Security | — | T |
