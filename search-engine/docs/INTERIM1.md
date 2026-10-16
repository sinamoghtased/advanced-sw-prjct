# Quickdex — Interim Project I

**CS/SE 6362 · Advanced Software Architecture and Design · Fall 2026 · UT Dallas**

**Team:** Yasin Sazid · Alessandro Botta · Sina Moghtased

**Date:** 26 September 2026

**Status:** Interim I architecture with requirements revised for the Phase I final submission on 15 October 2026; prototype implementation is in this repository.

## 1. Overview

Quickdex generates and alphabetizes circular shifts of input lines. The webpage
shows input, circular shifts, and alphabetized output, updating after each line.

## 2. Functional requirements

| ID | Requirement |
| --- | --- |
| FR001 | The system shall accept lines of text typed or pasted into a website text box. |
| FR002 | The system shall accept Unicode text and preserve each word's characters. |
| FR003 | The system shall accept between 1 and 10,000 characters of nonblank input. |
| FR004 | The system shall generate all circular shifts of each input line. |
| FR005 | The system shall display circular shifts in generation order alongside the alphabetized index. |
| FR006 | The system shall display the shifts in ascending alphabetical order, ignoring case. |
| FR007 | The system shall display the input alongside the circular shifts and alphabetized output. |
| FR013 | The system shall update the displayed results after each line, retaining results from earlier lines. |
| FR014 | The system shall retrieve pages from the KWIC index using case-insensitive keyword-prefix matching. |
| FR015 | The system shall rank search results by relevance and display matching index entries and available page links. |
| FR016 | The system shall suggest completions of the search while the user types. |
| FR017 (optional) | The system shall support plain-text and CSV export through its API and command-line interface. |

FR008–FR012 are listed in Section 4.2. Index noise-word filtering and live web sources remain outside the Phase I scope.

### Input and search rules

- Whitespace separates words. A leading HTTP(S) URL, or otherwise a trailing HTTP(S) URL, is stored as metadata and excluded from circular shifts.
- Blank and URL-only lines are skipped. A submission with no indexed words is rejected. The input limit counts Unicode code points, including whitespace.
- Indexed words retain their characters, case, and punctuation. Whitespace separators are normalized in displayed shifts.
- Duplicate words and lines are retained. A line with n words after URL extraction produces n shifts. Equal case-insensitive sort keys retain generation order.
- Search uses distinct lowercase query terms and ignores common words unless all query terms are common words. It returns pages matching all terms when available; otherwise it returns pages matching the greatest positive number of terms and identifies missing terms.
- Results are ranked by exact title, phrase-prefix match, whole-word count, phrase-prefix match at the beginning of the original line, case-insensitive alphabetical original text, then source-line number. Phrase matching uses circular shifts and may wrap from the end of a line to its beginning.
- Prefix matching preserves punctuation: `architecture` matches `architecture,` but not `(architecture)`; `architecture,` does not match `architecture`. Title and whole-word ranking comparisons strip surrounding punctuation.

## 3. Non-functional requirements

| ID | Quality | Requirement |
| --- | --- | --- |
| NFR001 | Understandability | The system shall document each component's responsibility, public operations, and place in the processing flow. |
| NFR002 | Portability | The system shall work across operating systems and web browsers. |
| NFR003 | Enhanceability | The system shall support new features through documented component interfaces. |
| NFR004 | Reusability | The KWIC engine shall be reusable independently of the web interface. |
| NFR005 | Performance | The system shall generate and display results quickly. |
| NFR006 | Usability | The system shall be easy for a first-time user to operate. |
| NFR007 | Responsiveness | The system shall display processing status within 1 second of submission and update the index during processing. |
| NFR008 | Adaptability | Changes shall remain localized to the affected components. |

## 4. Traceability to refined requirements

### 4.1 NFRs to more specific NFRs

| Parent | Refined ID | Requirement |
| --- | --- | --- |
| NFR002 | NFR009 | The system shall support Chrome and Firefox on Windows, macOS, and Linux. |
| NFR005 | NFR010 | The system shall display complete results within 3 seconds for supported input under a stable network connection. |
| NFR006 | NFR011 | A first-time user shall complete a submission and understand the results within 5 minutes without a separate manual. |
| NFR004 | NFR012 | The KWIC engine shall run without browser or HTTP services. |
| NFR008 | NFR013 | The sorting implementation shall be replaceable without changing other components or its public interface. |

### 4.2 NFRs to additional FRs

| Parent | Derived ID | Functional requirement |
| --- | --- | --- |
| NFR001, NFR006 | FR008 | The system shall label the input, circular-shift, and alphabetized-output areas. |
| NFR006 | FR009 | The system shall provide on-page instructions. |
| NFR006 | FR010 | The system shall allow users to copy text from both result areas. |
| NFR007 | FR011 | The system shall display a processing status. |
| NFR006, NFR007 | FR012 | The system shall report input or processing errors and allow the user to retry. |

### 4.3 Acceptance criteria for revised NFRs

These checks are planned acceptance criteria; their results have not yet been recorded.

| NFR | Acceptance check |
| --- | --- |
| NFR001 | Inspect every component for a stated responsibility and every public operation for a descriptive name, parameters, and return values. Confirm that the processing diagram shows all component interactions. |
| NFR003 | Walk through adding index noise-word filtering: identify affected components and interfaces, explain how filtered shifts reach Alphabetizer, and specify regression checks for ordering, source-line references, and incremental updates. Preserve the documented contracts of unaffected components. |
| NFR007 | On localhost with slow motion disabled, submit 100 copies of `alpha beta gamma delta epsilon zeta eta theta iota kappa`, separated by newlines (5,699 code points, no trailing newline). Record the browser and machine. Verify visible status within 1 second, one server update per processed line, and intermediate browser results. Record server delivery and browser display separately; browser updates may combine lines. |

For NFR003, Alphabetizer currently reads CircularShift directly. A filter therefore needs an explicit integration design; adding it is not assumed to require only one new component or one controller change.

## 5. Architectural style and components

**Style:** object-oriented / Abstract Data Types (ADT).

**Stack:** Next.js front end and Java back end.

SAS = Software Architecture Specification.

| ID | Component | Responsibility |
| --- | --- | --- |
| SAS001 | Input Medium | Accept user text |
| SAS002 | LineStorage | Store lines and hide their representation |
| SAS003 | MasterControl | Coordinate processing one line at a time |
| SAS004 | Input | Read, validate, and store input |
| SAS005 | CircularShift | Generate each line's circular shifts |
| SAS006 | Alphabetizer | Merge new shifts into the sorted index |
| SAS007 | Output | Retrieve and display results |
| SAS008 | Output Medium | Present results to the user |
| SAS009 | SearchEngine | Find and rank pages and suggest query completions |

## 6. Connections

| Caller | Callee |
| --- | --- |
| `MasterControl.run()` (diagram: `main()`) | `Input.setInput()` |
| `Input.setInput()` | `Input.storeInputInLineStorage()` |
| `Input.storeInputInLineStorage()` | `LineStorage.setCharacter()` |
| `MasterControl.run()` (diagram: `main()`) | `CircularShift.setLines()` |
| `CircularShift.setLines()` | `LineStorage.getCharacter()` |
| `CircularShift.setLines()` | `LineStorage.getWordCount()` |
| `CircularShift.setLines()` | `CircularShift.generateShifts()` |
| `MasterControl.run()` (diagram: `main()`) | `Alphabetizer.setShifts()` |
| `Alphabetizer.setShifts()` | `CircularShift.getShiftedCharacter()` |
| `Alphabetizer.setShifts()` | `CircularShift.getShiftedWordCount()` |
| `Alphabetizer.setShifts()` | `Alphabetizer.alphabetize()` |
| `MasterControl.run()` (diagram: `main()`) | `Output.getOutput()` |
| `Output.getOutput()` | `Alphabetizer.getAlphabetizedShift()` |
| `Output.getOutput()` | `CircularShift.getShiftedCharacter()` |
| `Output.getOutput()` | `CircularShift.getShiftedWordCount()` |
| `Output.getOutput()` | `Output.displayOutput()` |

**System I/O:** Input Medium → Input; Output → Output Medium.

`Output` retrieves generated-order shifts from `CircularShift` and sorted shifts from `Alphabetizer` before sending both to the Output Medium.

The diagram names the controller operation `main()`; the Java instance method is `run()`. The
static `main(String[])` method is the command-line launcher.

## 7. Constraints

- Process one line at a time.
- Retain and merge earlier alphabetized shifts.
- Update output after each line.
- Hide internal data and algorithms behind public operations.

## 8. Architecture diagram

![Team's Quickdex ADT architecture with SAS IDs](Quickdex-architecture-SAS.png)

Green = public methods; orange = private methods; black arrows = calls; red = system I/O.
SAS009 (SearchEngine) supports FR014–FR016. It obtains the sorted index through `Output.getOutput()`; MasterControl coordinates search and the web interface supplies queries and displays results.

## 9. Requirements-to-architecture traceability

### 9.1 Functional requirements

| FR | Architectural elements |
| --- | --- |
| FR001 | SAS001, SAS004 |
| FR002 | SAS002, SAS004 |
| FR003 | SAS004, SAS008 |
| FR004 | SAS002, SAS003, SAS005 |
| FR005 | SAS005, SAS007, SAS008 |
| FR006 | SAS006, SAS007, SAS008 |
| FR007 | SAS001, SAS003, SAS007, SAS008 |
| FR008 | SAS001, SAS008 |
| FR009 | SAS001 |
| FR010 | SAS008 |
| FR011 | SAS001, SAS003, SAS008 |
| FR012 | SAS001, SAS003, SAS008 |
| FR013 | SAS003, SAS005, SAS006, SAS007, SAS008 |
| FR014 | SAS001, SAS007, SAS008, SAS009 |
| FR015 | SAS007, SAS008, SAS009 |
| FR016 | SAS001, SAS008, SAS009 |
| FR017 (optional) | SAS003, SAS007 |

### 9.2 Non-functional requirements

| NFRs | Architectural elements |
| --- | --- |
| NFR001 | SAS001–SAS009 |
| NFR002, NFR009 | SAS001, SAS008 |
| NFR003 | SAS003–SAS007, SAS009 |
| NFR004, NFR012 | SAS002–SAS006 |
| NFR005, NFR010 | SAS002–SAS008 |
| NFR006, NFR011 | SAS001, SAS008 |
| NFR007 | SAS003, SAS007, SAS008 |
| NFR008, NFR013 | SAS002, SAS004, SAS005, SAS006 |

## 10. Trade-off analysis

**Reference:** [Shaw and Garlan ADT](https://www.cs.cmu.edu/~ModProb/KWICsol2.html).
**Alternative:** Quickdex ADT.

In = Input/LineStorage; CS = CircularShift; Alpha = Alphabetizer; Out = Output.

| Criterion | Reference In | Reference CS | Reference Alpha | Reference Out | Quickdex In | Quickdex CS | Quickdex Alpha | Quickdex Out |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Understandability | + | + | + | + | ++ | ++ | ++ | ++ |
| Algorithm modifiability | + | + | + | + | + | + | ++ | + |
| Data modifiability | ++ | + | + | 0 | ++ | + | + | 0 |
| Enhanceability | 0 | + | + | 0 | 0 | + | + | 0 |
| Reusability | + | + | + | 0 | + | + | + | 0 |
| Responsiveness: first useful output | 0 | 0 | 0 | − | 0 | + | + | + |
| Performance: processing overhead | 0 | − | − | + | 0 | − | − | − |
| Performance: memory economy | 0 | − | − | 0 | 0 | − | − | 0 |
| Portability: OS/browser | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| **Component total** | **+5** | **+3** | **+3** | **+2** | **+6** | **+5** | **+6** | **+3** |

**Architecture totals:** reference **+13**, Quickdex **+20**.
Totals summarize equally weighted design judgments for this scenario; they are not benchmarks.
