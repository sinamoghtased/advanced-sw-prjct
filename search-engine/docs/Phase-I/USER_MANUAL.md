# Quickdex User Manual (Preliminary)

This is a guide to *using* Quickdex, the search engine — not to its architecture or requirements
(see [`Arch-design-decisions.md`](Arch-design-decisions.md) and [`INTERIM1.md`](INTERIM1.md) for those). It
mirrors the [`/manual`](../frontend/src/app/manual/page.tsx) page in the app, which you can open
from the navbar.

This is a **Phase I / preliminary** manual: it covers what's built today. It will grow as more
requirements are implemented.

---

## 1. What Quickdex is

Quickdex is a search engine built on a **Key Word In Context (KWIC)** index. Every page it can
find you is broken into words, and every word is rotated to the front once (a "circular shift"),
then all of those rotations are sorted alphabetically. Searching means finding rotations that
start with your words — that's why every search word must **start** a word on the page, rather
than appear anywhere in it.

In Phase I, results come only from the engine's own built-in index
([`corpus.txt`](../backend/src/main/resources/corpus.txt)) — no live internet lookups.

## 2. Searching (the home page)

1. Go to the home page. Type a word or a few words into the search box.
2. A dropdown of **suggestions** appears as you type, completing what you've typed so far.
   - Use `↓` / `↑` to move through suggestions, `Enter` to pick one, `Esc` to close the list.
   - Press `Enter` in the box (or click **Search**) to search exactly what you typed.
3. Results are ranked, best first:
   1. An exact match of the page's title.
   2. An exact phrase match (your words appear together, in order).
   3. Pages with more matching words, then pages where your words appear earlier.
   4. Alphabetical order, as a final tie-break.
4. Each result shows:
   - A **breadcrumb** (the site and path) and a linked title, with your search words in **bold**.
   - An **"Exact phrase"** badge when your words matched as a phrase.
   - Up to two **KWIC matches** — the rotated context around your keyword — with a "+N more" link
     if there are more.
   - **Missing:** any of your words that no result fully contains, struck through, when no page
     matched all of them.
5. Use **Previous** / **Next** or the page numbers at the bottom to page through results.

If the backend isn't running, the page shows a message telling you to start it (see the
[README](../README.md#run-locally)).

## 3. Building your own index (the Indexer tab)

The Indexer tab lets you build a KWIC index from *your own* text and watch it grow, line by line —
this is the same pipeline that builds the home page's index, just visible.

1. Open **Indexer** in the navbar.
2. Enter text: type or paste lines directly. One line = one entry; a web address at the start or
   end of a line becomes that entry's link.
   - Limit: 10,000 Unicode code points, including whitespace, shown live under the box.
3. Click **Create index**. Watch:
   - The **circular shifts** table, listing every rotation generated, in the order it was created.
   - The **alphabetized table**, updating after each line is merged in.
   - **Copy shifts** and **Copy index** buttons for the two result tables.
   - Turn on **Slow motion** to watch it build one line at a time instead of all at once.
4. **Filter** the alphabetized table to find specific entries once it's built.
5. Click **"Search these pages in the search engine →"** to open your own index in the same
   search UI described in §2 — it searches only the pages you entered.

## 4. About page

The **About** tab describes the project, the seven components of the KWIC/search architecture,
and the team. It links to the project plan PDF and back to the search engine.

## 5. Known limitations (Phase I)

- Search matches are **prefix matches on whole words**, not substring or fuzzy matches. Typing
  "arch" will match "architecture" but typing "chitecture" will not.
- Live web sources (Wikipedia, Hacker News, Stack Overflow, arXiv) exist in the code but are
  **off by default**; Phase I answers only from `corpus.txt`. See the
  [frontend README](../frontend/README.md#online-sources) if you want to turn them on.
- Indexes are **in memory only** — restarting the backend rebuilds the built-in index from
  `corpus.txt` and clears any indexes built in the Indexer tab.

## 6. Getting help

- **The app says the backend isn't running:** follow [Run locally](../README.md#run-locally) in
  the main README, starting the backend first.
- **Something looks wrong or missing:** open an issue on
  [GitHub](https://github.com/sinamoghtased/advanced-sw-prjct), or contact the team (see the
  About page).
