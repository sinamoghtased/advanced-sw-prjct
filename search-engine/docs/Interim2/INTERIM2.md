# Cyberminer — Interim Project II

**CS/SE 6362 · Advanced Software Architecture and Design · Fall 2026 · UT Dallas**

**Team:** Yasin Sazid · Alessandro Botta · Sina Moghtased

**Project plan:** [Interim Project II](Cyberminer_Interim_II_Project_Plan.pdf)

**Status:** Requirements draft. The [project plan, §2.2](Cyberminer_Interim_II_Project_Plan.pdf) records which features the team reported as complete, partial, or planned at the interim.

## 1. Overview

Cyberminer uses the Quickdex KWIC engine to index web-page descriptions with their URLs. Users enter keywords on the Search page and open matching pages from the results. The Indexer shows the input, the circular shifts, the filtered shifts, and the alphabetized index.

## 2. Functional requirements

| ID | Requirement |
| --- | --- |
| FR001 | The system shall accept lines of text typed or pasted into a website text box. |
| FR002 | &emsp;↳ *FR001* · The system shall accept Unicode text and preserve each word's characters. |
| FR003 | &emsp;↳ *FR001* · The system shall accept between 1 and 10,000 characters of nonblank input. |
| FR004 | The system shall generate all circular shifts of each input line. |
| FR018 | &emsp;↳ *FR004* · The system shall exclude shifts that start with a noise word, including `a`, `the`, and `of`, from the filtered output and the alphabetized index. |
| FR006 | &emsp;↳ *FR004* · The system shall display the filtered shifts in ascending alphabetical order, ignoring case. |
| FR007 | The Indexer shall show four areas: input, raw shifts in generation order, shifts after noise-word filtering, and the alphabetized index. |
| FR005 | &emsp;↳ *FR007* · The system shall display circular shifts in generation order alongside the alphabetized index. |
| FR013 | &emsp;↳ *FR007* · The system shall update the displayed results after each line, retaining results from earlier lines. |
| FR019 | Cyberminer shall use the KWIC engine to maintain a searchable collection of URLs and their descriptions. |
| FR020 | &emsp;↳ *FR019* · The system shall validate each URL and require a nonblank description before adding a page to the searchable collection. |
| FR021 | &emsp;↳ *FR019* · Build mode shall create the initial index from the supplied page list. |
| FR022 | &emsp;↳ *FR019* · Grow mode shall add pages to the existing index and preserve earlier entries. |
| FR023 | &emsp;↳ *FR019* · An administrator shall be able to delete an outdated URL together with its description and associated KWIC shifts. |
| FR024 | &emsp;&emsp;↳ *FR023* · The system shall ask the administrator to confirm deletion. If the administrator cancels, the page shall remain in the index. |
| FR014 | The system shall retrieve URLs by matching query keywords against word prefixes in the indexed descriptions. |
| FR025 | &emsp;↳ *FR014* · Users shall be able to select OR (default), AND, or NOT as the search mode. |
| FR026 | &emsp;↳ *FR014* · Users shall be able to enable and use case-sensitive matching. |
| FR027 | &emsp;↳ *FR014* · The user shall be able to specify which symbols to ignore when matching queries to descriptions. |
| FR016 | &emsp;↳ *FR014* · The system shall suggest completions of the search while the user types. |
| FR028 | &emsp;&emsp;↳ *FR016* · While suggesting completions, the system shall offer corrections for mistyped keywords using words in the index. |
| FR029 | &emsp;↳ *FR014* · When online sources are enabled, Cyberminer shall search them alongside the local index. |
| FR015 | The Search page shall display matching entries in the selected order. |
| FR030 | &emsp;↳ *FR015* · Users shall sort results alphabetically, by most clicks or by highest bid per URL. |
| FR031 | &emsp;↳ *FR015* · The user shall be able to set the number of results per page. |
| FR032 | &emsp;↳ *FR015* · Selecting a result URL shall open the corresponding website. |
| FR033 | &emsp;↳ *FR015* · Users shall be able to navigate between pages of search results. |
| FR017 | The system shall support plain-text and CSV export through its API and command-line interface. |

**Legend:** On the FR002 row, `↳ FR001` means FR002 adds detail to FR001.

FR008–FR012 are listed in Section 4.2.

### Input and search rules

- The initial page list comes from `corpus.txt`. Additional records come from the Indexer and from online sources when enabled. Online sources are off by default.
- A searchable record consists of a URL and its description. The supported URL grammar is `url = "http://" identifier "." identifier "." suffix`, with `identifier = (letter | digit)+` and `suffix = "edu" | "com" | "org" | "net"`. For example, `http://www.utdallas.edu` matches this grammar. The collection excludes government, military, and foreign sites. HTTPS and additional subdomains are pending team agreement.
- In the Indexer, whitespace separates words. A leading URL, or otherwise a trailing URL, is stored separately from the words to be shifted. Plain text can be used for a standalone KWIC submission. Blank and URL-only lines are skipped. A submission with no indexed words is rejected. The FR003 limit applies to each manual Indexer submission and counts Unicode code points, including whitespace.
- Indexed words retain their case and punctuation. Displayed shifts normalize whitespace. Each shift of a searchable description retains its source URL. Duplicate words and lines are retained. A line with `n` words after URL extraction produces `n` raw shifts. Equal case-insensitive sort keys retain generation order.
- For the description `the blue sky`, the raw view shows `the blue sky`, `blue sky the`, and `sky the blue`. The alphabetized index contains `blue sky the` and `sky the blue`, each with the source URL.
- Search treats repeated query terms as one term. For a query of `software architecture`, OR returns pages described as either `software architecture` or `software testing`. AND returns only the former. Changing the result order preserves the set of matching URLs.

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
| NFR014 | Configurability | Users shall be able to change search preferences directly in the web interface. |

## 4. Traceability to refined requirements

### 4.1 NFRs to more specific NFRs

| Parent | Refined ID | Requirement |
| --- | --- | --- |
| NFR002 | NFR009 | The system shall support Chrome and Firefox on Windows, macOS, and Linux. |
| NFR005 | NFR010 | The Indexer shall display complete results within 3 seconds for supported input under a stable network connection. |
| NFR006 | NFR011 | A first-time user shall complete a submission and understand the results within 5 minutes without a separate manual. |
| NFR004 | NFR012 | The KWIC engine shall run without browser or HTTP services. |
| NFR008 | NFR013 | The sorting implementation shall be replaceable without changing other components or its public interface. |

### 4.2 NFRs to additional FRs

| Parent | Derived ID | Functional requirement |
| --- | --- | --- |
| NFR001, NFR006 | FR008 | The Indexer shall label the input, circular-shift, filtered-shift, and alphabetized-output areas. |
| NFR006 | FR009 | The system shall provide on-page instructions. |
| NFR006 | FR010 | The Indexer shall allow users to copy the circular shifts and the alphabetized output. |
| NFR007 | FR011 | The system shall display a processing status. |
| NFR006, NFR007 | FR012 | The system shall report input or processing errors and allow the user to retry. |

For FR009, users can also open the manual from the site navigation.

NFR014 is realized by FR025–FR027 and FR030–FR031.

### 4.3 Acceptance criteria for revised NFRs

| NFR | Acceptance check |
| --- | --- |
| NFR001 | Inspect every component for a stated responsibility and every public operation for a descriptive name, parameters, and return values. Confirm that the processing diagram shows all component interactions. |
| NFR003 | Trace a change to the noise-word filter from shift generation to the alphabetized index. Verify ordering, source URLs, and incremental updates against the documented interfaces. |
| NFR007 | On localhost with slow motion disabled, submit 100 copies of `alpha beta gamma delta epsilon zeta eta theta iota kappa`, separated by newlines (5,699 code points, no trailing newline). Record the browser and machine. Verify visible status within 1 second, one server update per processed line, and intermediate browser results. Record server delivery and browser display separately; browser updates may combine lines. |
| NFR014 | Run the same query before and after changing each search setting through the web interface. Check membership for mode, case, and symbol settings, order for the ranking setting, and the number of visible results for page size. |
