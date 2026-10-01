# Technologies mapped to the architecture rationale

| Technology | Maps to | Why |
| ---------- | ------- | --- |
| Plain JDK `HttpServer`, no framework | Information hiding | Nothing auto-exposes your classes; only what you make public is public. |
| Narrow public methods per component | Modifiability of algorithms | One or two methods per class- swap an internal algorithm, nothing else breaks. |
| `InputMedium` / `OutputMedium` interfaces | Reusability | Same pipeline classes run unmodified for both the CLI and the web server. |
| Streaming NDJSON over one HTTP response | Output timing / incremental merge | One JSON line out per line processed- the constraint, implemented directly. |
| Next.js API routes as a thin proxy | Modifiability of data | Frontend only sees JSON; backend storage can change freely behind it. |
| JUnit 5, one test class per component | Reusability + information hiding | Proves the separation is real- isolated components are what makes isolated tests possible. |
| Separate Maven / npm builds | Reusability, at the deployment level | Two independent toolchains mirror the component separation in code. |
| Docker multi-stage build | Enhanceability (deployment) | Same image runs on any container host, no code changes. |
| Vercel + Render, one env var | Modifiability, at the infra level | Swap either host or the backend's language entirely; only the HTTP contract matters. |
| TypeScript, Tailwind CSS |- | General frontend tooling, not part of the ADT rationale. |
