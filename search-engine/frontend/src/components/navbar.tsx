"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const tabs = [
  { href: "/", label: "Search" },
  { href: "/indexer", label: "Indexer" },
  { href: "/manual", label: "Manual" },
  { href: "/tech", label: "Tech" },
  { href: "/about", label: "About" },
];

const links = [
  { href: "/project-plan.pdf", label: "Project Plan" },
  { href: "https://github.com/sinamoghtased/advanced-sw-prjct", label: "GitHub" },
];

export default function Navbar() {
  const pathname = usePathname();
  return (
    <header className="sticky top-0 z-10 border-b border-black/[.08] bg-background/80 backdrop-blur dark:border-white/[.145]">
      <nav className="mx-auto flex min-h-14 w-full max-w-5xl flex-wrap items-center justify-between gap-x-6 px-4 sm:px-6">
        <Link href="/" className="py-3 font-semibold tracking-tight">
          Quickdex
        </Link>
        <div className="-mx-2.5 flex items-center gap-0.5 overflow-x-auto text-sm sm:mx-0 sm:gap-2">
          {tabs.map((tab) => {
            const active = tab.href === "/" ? pathname === "/" : pathname.startsWith(tab.href);
            return (
              <Link
                key={tab.href}
                href={tab.href}
                aria-current={active ? "page" : undefined}
                className={`whitespace-nowrap rounded-full px-2.5 py-1.5 transition-colors sm:px-3 ${
                  active
                    ? "bg-black/[.06] font-medium text-foreground dark:bg-white/[.1]"
                    : "text-zinc-600 hover:text-foreground dark:text-zinc-400"
                }`}
              >
                {tab.label}
              </Link>
            );
          })}
          {links.map((link) => (
            <a
              key={link.href}
              href={link.href}
              target="_blank"
              rel="noopener noreferrer"
              className="whitespace-nowrap rounded-full px-2.5 py-1.5 text-zinc-600 transition-colors hover:text-foreground sm:px-3 dark:text-zinc-400"
            >
              {link.label}
            </a>
          ))}
        </div>
      </nav>
    </header>
  );
}
