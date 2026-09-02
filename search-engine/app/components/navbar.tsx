import Link from "next/link";

export default function Navbar() {
  return (
    <header className="sticky top-0 z-10 border-b border-black/[.08] bg-background/80 backdrop-blur dark:border-white/[.145]">
      <nav className="mx-auto flex h-14 w-full max-w-5xl items-center justify-between px-6">
        <Link href="/" className="font-semibold tracking-tight">
          KWIC
        </Link>
        <div className="flex items-center gap-6 text-sm">
          <a
            href="/project-plan.pdf"
            target="_blank"
            rel="noopener noreferrer"
            className="text-zinc-600 transition-colors hover:text-foreground dark:text-zinc-400"
          >
            Project Plan
          </a>
          <a
            href="https://github.com/"
            target="_blank"
            rel="noopener noreferrer"
            className="text-zinc-600 transition-colors hover:text-foreground dark:text-zinc-400"
          >
            GitHub
          </a>
        </div>
      </nav>
    </header>
  );
}
