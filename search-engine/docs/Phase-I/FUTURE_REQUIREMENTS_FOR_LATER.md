# Quickdex — Future requirements for later

**Status: deferred candidates — not part of Interim Project I.**

This file preserves possible extensions removed from the Interim I baseline.
They are ideas for later discussion, not approved implementation commitments or a
complete Phase II specification. Confirm the Phase II assignment before selecting
its requirements. `LATER` IDs are separate from the active FR/NFR numbering.

## Candidate requirements

| ID | Candidate for later | Notes |
| --- | --- | --- |
| LATER001 | Search the generated index by keyword or keyword prefix. | Search belongs in later search-engine planning; query semantics need definition. |
| LATER002 | Associate entries with URLs and let users open the corresponding pages. | Define how text and URLs enter the system and remain associated. |
| LATER003 | Remove shifts beginning with a configured noise word when filtering is enabled. | If implemented, add a fourth visible area showing noise-filtered shifts, as Lawrence requested. |
| LATER004 | Allow users to choose case-sensitive or case-insensitive ordering. | Define ordering and tie behavior for each mode. |
| LATER005 | Accept UTF-8 plain-text file uploads. | Define file-size limits and invalid-encoding behavior. |
| LATER006 | Load built-in sample input. | Optional convenience for demonstrations. |
| LATER007 | Export results as plain text or CSV. | Define fields and encoding; protect CSV exports against formula interpretation. |
| LATER008 | Paginate large result sets and show source-line numbers. | Revisit only when the expected input and result sizes justify it. |
| LATER010 | Provide diagnostic logs of processed lines, shifts, index size, and elapsed time. | Development support; does not replace the visible intermediate-shift area. |
| LATER011 | Change input limits, noise words, and default sorting through configuration. | Define who controls these settings and when changes take effect. |

LATER009 has been retired: incremental processing, merging, and per-line output
updates belong to the current Interim I design (FR013 and §7 of INTERIM1.md).
The remaining IDs are retained for stable references.

## Quality targets to revisit later

Broader browser coverage, large-input throughput, filter latency, detailed responsive
layout targets, and availability targets require an agreed workload and test conditions.
The previous draft's 99% uptime and universal 100 ms interaction promises are not
carried forward as commitments. Likewise, extension through exactly one new class
is not assumed to be achievable for every future feature.

The current baseline is maintained in [INTERIM1.md](INTERIM1.md).
