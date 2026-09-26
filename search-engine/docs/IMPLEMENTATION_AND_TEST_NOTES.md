# Quickdex — Implementation and test notes

These notes support the current Interim I requirements. They are separate from
presentation content and from future features. The numerical limits and timing
values are proposed targets to validate, not measured results.

## Input and output rules

| Requirements | Working rule |
| --- | --- |
| FR001–FR002 | Newlines separate lines; spaces and tabs separate words. Ignore blank lines and repeated separators. Preserve each word's original characters and case. |
| FR003 | Count the 10,000-character limit in Unicode code points, including whitespace. Reject empty or whitespace-only input and explain the limit when it is exceeded. |
| FR004 | A line with n words produces n shifts, including the original order. Retain duplicates. |
| FR005 | Group shifts by source line. Show the original order first, followed by successive one-word shifts. |
| FR006 | Proposed ordering: lexicographic Unicode code-point order after locale-independent lowercase conversion. Preserve original case in output and generation order for equal comparison values. |
| FR007, FR013 | Start each submission with a fresh index and clear previous results. Accumulate the current submission's results after each line. |
| FR011 | Keep processing status visible until completion or failure and prevent overlapping submissions. |
| FR012 | Preserve input on error and allow retry. Do not present stale or partial results as complete. |

## Implementation decisions

- Specify each method's parameters and return values before implementing callers.
- Complete Output's retrieval path for unsorted shifts, as noted in INTERIM1 §6.
- Validate input before generation; keep state separate between submissions.
- Preserve earlier alphabetized shifts and merge each new line's shifts into them.
  Do not regenerate earlier lines' shifts. The presentation proposes merge-based sorting.
- Deliver updates to the browser during processing; choose the transport mechanism.
- Render user text as text, not HTML or executable code.
- Select Java service hosting and verify its connection to the planned Vercel front end.

## Verification

| Area | Check |
| --- | --- |
| Correctness | One line, multiple lines, repeated words, blank input, extra whitespace, mixed case, Unicode, and the input-size boundary. |
| Incremental output | Both result areas update after each line; earlier shifts remain present; the final index contains every shift. |
| Error handling | Invalid input and processing failures show messages and permit retry without misleading old results. |
| NFR009 | Record tested Chrome/Firefox and Windows/macOS/Linux versions. |
| NFR010 | Measure from submission to the final rendered update. Record input, server resources, browser, and network conditions against the 3-second target. |
| NFR011 | Observe a first-time user submitting text and identifying both result areas within 5 minutes without a separate manual. |
| NFR012 | Run the core from a non-web test program without browser or HTTP services. |
| NFR013 | Replace the sorting implementation while preserving its contract; check whether other components need changes. |

## Architecture comparison evidence

Use the same input and ordering rules when comparing Quickdex with the reference
ADT implementation. Measure first useful output, complete output, and peak memory
separately. Incremental output may arrive earlier while total completion costs more.
Neither incremental nor batch execution inherently requires disk access.

For data modifiability, replace the storage representation and record affected
interfaces and callers. For enhanceability, examine adding noise filtering or search
as an extension scenario. Keep design judgments separate from measured results.
