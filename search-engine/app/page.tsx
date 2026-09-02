const team = [
  { name: "Yasin Sazid", email: "yasin.sazid@utdallas.edu" },
  { name: "Alessandro Botta", email: "alessandro.botta@utdallas.edu" },
  { name: "Sina Moghtased", email: "sina.moghtased@utdallas.edu" },
];

export default function Home() {
  return (
    <main className="mx-auto flex w-full max-w-5xl flex-1 flex-col justify-center gap-16 px-6 py-24">
      <section className="flex flex-col gap-6">
        <p className="text-sm font-medium uppercase tracking-widest text-zinc-500">
          CS 6362 &middot; Advanced Software Architecture
        </p>
        <h1 className="max-w-2xl text-4xl font-semibold leading-tight tracking-tight sm:text-5xl">
          A KWIC indexing search engine.
        </h1>
        <p className="max-w-2xl text-lg leading-8 text-zinc-600 dark:text-zinc-400">
          An object-oriented Key Word In Context system that generates circular
          word shifts from input text and displays them in alphabetical order,
          forming the core of a web-based search engine.
        </p>
        <div className="flex flex-col gap-3 sm:flex-row">
          <a
            href="/project-plan.pdf"
            target="_blank"
            rel="noopener noreferrer"
            className="flex h-11 items-center justify-center rounded-full bg-foreground px-6 text-sm font-medium text-background transition-colors hover:bg-[#383838] dark:hover:bg-[#ccc]"
          >
            Read the project plan
          </a>
        </div>
      </section>

      <section className="flex flex-col gap-4">
        <h2 className="text-sm font-medium uppercase tracking-widest text-zinc-500">
          Team
        </h2>
        <ul className="grid gap-4 sm:grid-cols-3">
          {team.map((member) => (
            <li
              key={member.email}
              className="rounded-lg border border-black/[.08] p-4 dark:border-white/[.145]"
            >
              <p className="font-medium">{member.name}</p>
              <a
                href={`mailto:${member.email}`}
                className="text-sm text-zinc-600 hover:underline dark:text-zinc-400"
              >
                {member.email}
              </a>
            </li>
          ))}
        </ul>
      </section>
    </main>
  );
}
