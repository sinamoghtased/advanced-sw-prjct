# Quickdex backend

The Java backend: the KWIC engine and search engine, built with the Abstract Data Type architecture
described in [`../docs/Arch-design-decisions.md`](../docs/Arch-design-decisions.md), and a small HTTP
API that the [frontend](../frontend) calls. It uses only the Java standard library; JUnit is used
for tests.

## Requirements

Java 21 or newer, and Maven.

## Build, run, and test

From this folder:

```bash
mvn package -DskipTests            # build target/quickdex-engine.jar
java -jar target/quickdex-engine.jar   # start the HTTP API on port 8080
mvn test                           # run the automated tests
```

The server is ready when it prints `Quickdex engine listening on http://localhost:8080`. To use
another port, pass it as an argument (`java -jar target/quickdex-engine.jar 9090`) or set `PORT`.

The engine also runs from the command line, without the web page (NFR3.0):

```bash
echo "Descent of Man" | java -cp target/quickdex-engine.jar edu.utdallas.quickdex.MasterControl
java -cp target/quickdex-engine.jar edu.utdallas.quickdex.MasterControl input.txt --search man
java -cp target/quickdex-engine.jar edu.utdallas.quickdex.MasterControl input.txt --csv
```

## Configuration

| Setting | Default | Purpose |
| ------- | ------- | ------- |
| `PORT` (or the first argument) | `8080` | Port for the HTTP API |
| `QUICKDEX_CORPUS` | [`corpus.txt`](src/main/resources/corpus.txt) | File of pages for the web index at startup: one per line, a URL then its description |

## HTTP API

| Method and path | Purpose |
| --------------- | ------- |
| `GET /search?q=WORDS` | Pages that match the search words, best first, `limit` at a time (default 10) from `offset`. Add `index=ID` to search an index built with `POST /index`. |
| `GET /suggest?q=TYPED` | Suggested searches that complete what the user typed |
| `POST /pages` | Adds pages (one "URL description" line each) to the web index; known URLs are skipped |
| `POST /index` | Builds a new index from the posted text and streams an update after every line, as newline-delimited JSON. `delay=MS` slows it down for demos. |
| `GET /export?index=ID&format=csv\|txt` | Downloads an index as a file |
| `GET /stats` | Number of pages and index entries (add `index=ID` for a built index) |
| `GET /health` | Reports that the server is running |

## Code layout

```
src/main/java/edu/utdallas/quickdex/
├── MasterControl.java      # Calls the other components once per line; logs each line
├── Input.java              # Reads, validates, and stores lines
├── LineStorage.java        # Stores the characters and words of every line
├── CircularShift.java      # Generates the circular shifts of each line
├── Alphabetizer.java       # Merges each line's shifts into the sorted index
├── Output.java             # Shows the index after every line; exports it
├── SearchEngine.java       # Finds and ranks matching pages; suggests searches
├── InputMedium.java, OutputMedium.java, TextInputMedium.java, QueueInputMedium.java
├── KwicEntry.java, PlacedEntry.java, SearchResult.java
└── api/                    # HTTP API: EngineServer, WebOutputMedium, Json
src/main/resources/corpus.txt   # Pages in the web index at startup
src/test/java/                  # JUnit tests, one class per component plus the HTTP API
```
