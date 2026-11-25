"use client";

import { useRouter } from "next/navigation";
import { createContext, useContext, useState } from "react";

type AdminState = {
  enabled: boolean;
  setEnabled: (enabled: boolean) => void;
  /** The admin key, kept in memory for this tab only. */
  key: string;
  setKey: (key: string) => void;
};

const AdminContext = createContext<AdminState>({
  enabled: false,
  setEnabled: () => {},
  key: "",
  setKey: () => {},
});

/** Holds whether admin tools are showing and the admin key the admin typed in. */
export function AdminProvider({ children }: { children: React.ReactNode }) {
  const [enabled, setEnabled] = useState(false);
  const [key, setKey] = useState("");
  return <AdminContext value={{ enabled, setEnabled, key, setKey }}>{children}</AdminContext>;
}

/** Shows or hides the admin tools on every result. */
export function AdminToggle() {
  const { enabled, setEnabled } = useContext(AdminContext);
  return (
    <button
      type="button"
      aria-pressed={enabled}
      onClick={() => setEnabled(!enabled)}
      className={`rounded-full border px-3 py-1 text-sm ${
        enabled
          ? "border-foreground bg-foreground text-background"
          : "border-black/[.12] dark:border-white/[.2]"
      }`}
    >
      Admin tools
    </button>
  );
}

type Action = "delete" | "bid";

/**
 * Delete and bid controls for one result (FR023, FR030). Deleting asks the admin to confirm
 * first. When the server has an admin key, the admin is asked for it and types it in.
 */
export function ResultAdmin({ url, index }: { url: string; index: string }) {
  const { enabled, key, setKey } = useContext(AdminContext);
  const router = useRouter();
  const [confirming, setConfirming] = useState(false);
  const [deleted, setDeleted] = useState(false);
  const [bid, setBid] = useState("");
  const [message, setMessage] = useState("");
  const [needsKey, setNeedsKey] = useState<Action | null>(null);
  const [keyInput, setKeyInput] = useState("");
  const [busy, setBusy] = useState(false);

  if (!enabled) return null;

  async function attempt(action: Action, adminKey: string) {
    const params = new URLSearchParams({ url });
    if (index) params.set("index", index);
    if (action === "bid") params.set("amount", bid);
    setBusy(true);
    try {
      const response =
        action === "delete"
          ? await fetch(`/api/pages?${params}`, { method: "DELETE", headers: { "x-admin-key": adminKey } })
          : await fetch(`/api/bid?${params}`, { method: "POST", headers: { "x-admin-key": adminKey } });
      if (response.status === 401) {
        setNeedsKey(action);
        setMessage(adminKey ? "Wrong admin key. Try again." : "Enter the admin key to continue.");
        return;
      }
      if (!response.ok) {
        const body = (await response.json().catch(() => ({}))) as { error?: string };
        setMessage(body.error ?? "That did not work. Try again.");
        setConfirming(false);
        return;
      }
      setNeedsKey(null);
      setKey(adminKey);
      if (action === "delete") {
        setDeleted(true);
        setMessage("Page deleted.");
        setTimeout(() => router.refresh(), 1200);
      } else {
        setMessage(`Bid set to ${bid}.`);
      }
    } catch {
      setMessage("The search engine could not be reached. Try again.");
    } finally {
      setBusy(false);
    }
  }

  const bidIsValid = /^\d{1,7}$/.test(bid);

  return (
    <div className="mt-1 flex flex-col gap-2 border-l-2 border-dashed border-zinc-300 pl-3 text-sm dark:border-zinc-600">
      {!deleted && (
        <div className="flex flex-wrap items-center gap-3">
          <label className="flex items-center gap-2">
            <span className="text-zinc-600 dark:text-zinc-400">Bid</span>
            <input
              type="number"
              inputMode="numeric"
              min={0}
              value={bid}
              onChange={(e) => setBid(e.target.value)}
              className="h-8 w-24 rounded-md border border-black/[.15] bg-transparent px-2 dark:border-white/[.25]"
            />
          </label>
          <button
            type="button"
            disabled={!bidIsValid || busy}
            onClick={() => void attempt("bid", key)}
            className="rounded-full border border-black/[.12] px-3 py-1 disabled:opacity-40 dark:border-white/[.2]"
          >
            Set bid
          </button>
          {confirming ? (
            <span className="flex flex-wrap items-center gap-2" role="alertdialog" aria-label="Confirm delete">
              <span className="font-medium">Delete this page and its description?</span>
              <button
                type="button"
                disabled={busy}
                onClick={() => void attempt("delete", key)}
                className="rounded-full bg-red-700 px-3 py-1 text-white disabled:opacity-40"
              >
                Yes, delete
              </button>
              <button
                type="button"
                onClick={() => {
                  setConfirming(false);
                  setNeedsKey(null);
                  setMessage("");
                }}
                className="rounded-full border border-black/[.12] px-3 py-1 dark:border-white/[.2]"
              >
                Cancel
              </button>
            </span>
          ) : (
            <button
              type="button"
              onClick={() => {
                setConfirming(true);
                setMessage("");
              }}
              className="rounded-full border border-red-700 px-3 py-1 text-red-700 dark:border-red-400 dark:text-red-400"
            >
              Delete
            </button>
          )}
        </div>
      )}
      {needsKey && (
        <form
          onSubmit={(e) => {
            e.preventDefault();
            void attempt(needsKey, keyInput);
          }}
          className="flex flex-wrap items-center gap-2"
        >
          <input
            type="password"
            autoComplete="off"
            value={keyInput}
            onChange={(e) => setKeyInput(e.target.value)}
            aria-label="Admin key"
            placeholder="Admin key"
            className="h-8 w-48 rounded-md border border-black/[.15] bg-transparent px-2 dark:border-white/[.25]"
          />
          <button
            type="submit"
            disabled={keyInput === "" || busy}
            className="rounded-full bg-foreground px-3 py-1 text-background disabled:opacity-40"
          >
            Continue
          </button>
        </form>
      )}
      <p role="status" className="min-h-5 text-zinc-600 dark:text-zinc-400">
        {message}
      </p>
    </div>
  );
}
