"use client";

/** Tells the engine a result was clicked, so results can be ordered by most clicks (FR030). */
function countClick(url: string, index: string) {
  const params = new URLSearchParams({ url });
  if (index) params.set("index", index);
  // keepalive lets the request finish even though the click opens another page.
  fetch(`/api/click?${params}`, { method: "POST", keepalive: true }).catch(() => {
    // Counting clicks is optional; ignore failures.
  });
}

/** A link to a result's web site that counts the click. */
export default function ResultLink({
  url,
  index,
  className,
  children,
}: {
  url: string;
  index: string;
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <a
      href={url}
      target="_blank"
      rel="noopener noreferrer"
      className={className}
      onClick={() => countClick(url, index)}
      onAuxClick={(e) => {
        if (e.button === 1) countClick(url, index);
      }}
    >
      {children}
    </a>
  );
}
