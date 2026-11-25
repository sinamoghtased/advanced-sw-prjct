import { act, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AdminProvider, AdminToggle, ResultAdmin } from "./admin";

const router = vi.hoisted(() => ({ push: vi.fn(), refresh: vi.fn() }));
vi.mock("next/navigation", () => ({ useRouter: () => router }));

const fetchMock = vi.fn();
const URL_ = "https://example.org/page";

function reply(status: number, body: object = {}) {
  return new Response(JSON.stringify(body), { status });
}

function callAt(n: number) {
  const [address, options] = fetchMock.mock.calls[n];
  const url = new URL(address, "http://localhost");
  return { path: url.pathname, params: url.searchParams, method: options.method, key: options.headers["x-admin-key"] };
}

async function openTools(index = "") {
  const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
  render(
    <AdminProvider>
      <AdminToggle />
      <ResultAdmin url={URL_} index={index} />
    </AdminProvider>,
  );
  await user.click(screen.getByRole("button", { name: "Admin tools" }));
  return user;
}

beforeEach(() => {
  vi.useFakeTimers({ shouldAdvanceTime: true });
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  vi.useRealTimers();
  fetchMock.mockReset();
  router.refresh.mockReset();
  vi.unstubAllGlobals();
});

describe("admin tools", () => {
  it("are hidden until Admin tools is pressed", async () => {
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
    render(
      <AdminProvider>
        <AdminToggle />
        <ResultAdmin url={URL_} index="" />
      </AdminProvider>,
    );
    expect(screen.queryByRole("button", { name: "Delete" })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Admin tools" })).toHaveAttribute("aria-pressed", "false");

    await user.click(screen.getByRole("button", { name: "Admin tools" }));
    expect(screen.getByRole("button", { name: "Delete" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Admin tools" })).toHaveAttribute("aria-pressed", "true");

    await user.click(screen.getByRole("button", { name: "Admin tools" }));
    expect(screen.queryByRole("button", { name: "Delete" })).not.toBeInTheDocument();
  });

  it("ask to confirm a delete, and Cancel keeps the page", async () => {
    const user = await openTools();
    await user.click(screen.getByRole("button", { name: "Delete" }));
    expect(screen.getByText("Delete this page and its description?")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Cancel" }));
    expect(screen.queryByText("Delete this page and its description?")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Delete" })).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("delete the page once confirmed, then refresh the results", async () => {
    fetchMock.mockResolvedValue(reply(200, { deleted: 1, pages: 135, entries: 810 }));
    const user = await openTools("abc");
    await user.click(screen.getByRole("button", { name: "Delete" }));
    await user.click(screen.getByRole("button", { name: "Yes, delete" }));

    await waitFor(() => expect(screen.getByRole("status")).toHaveTextContent("Page deleted."));
    const call = callAt(0);
    expect(call).toMatchObject({ path: "/api/pages", method: "DELETE", key: "" });
    expect(call.params.get("url")).toBe(URL_);
    expect(call.params.get("index")).toBe("abc");
    expect(screen.queryByRole("button", { name: "Delete" })).not.toBeInTheDocument();

    expect(router.refresh).not.toHaveBeenCalled();
    await act(async () => {
      vi.advanceTimersByTime(1500);
    });
    expect(router.refresh).toHaveBeenCalledTimes(1);
  });

  it("ask for the admin key when the server needs one, then retry with it", async () => {
    fetchMock.mockResolvedValueOnce(reply(401, { error: "Deleting a page needs the admin key." }));
    const user = await openTools();
    await user.click(screen.getByRole("button", { name: "Delete" }));
    await user.click(screen.getByRole("button", { name: "Yes, delete" }));

    expect(await screen.findByLabelText("Admin key")).toHaveAttribute("type", "password");
    expect(screen.getByRole("status")).toHaveTextContent("Enter the admin key to continue.");
    expect(screen.getByRole("button", { name: "Continue" })).toBeDisabled();

    fetchMock.mockResolvedValueOnce(reply(200, { deleted: 1 }));
    await user.type(screen.getByLabelText("Admin key"), "s3cret");
    await user.click(screen.getByRole("button", { name: "Continue" }));

    await waitFor(() => expect(screen.getByRole("status")).toHaveTextContent("Page deleted."));
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(callAt(1)).toMatchObject({ path: "/api/pages", method: "DELETE", key: "s3cret" });
    expect(screen.queryByLabelText("Admin key")).not.toBeInTheDocument();
  });

  it("say so when the admin key is wrong, and keep the page", async () => {
    fetchMock.mockResolvedValue(reply(401, { error: "Deleting a page needs the admin key." }));
    const user = await openTools();
    await user.click(screen.getByRole("button", { name: "Delete" }));
    await user.click(screen.getByRole("button", { name: "Yes, delete" }));
    await user.type(await screen.findByLabelText("Admin key"), "nope");
    await user.click(screen.getByRole("button", { name: "Continue" }));

    await waitFor(() => expect(screen.getByRole("status")).toHaveTextContent("Wrong admin key. Try again."));
    expect(screen.getByLabelText("Admin key")).toBeInTheDocument();
    expect(router.refresh).not.toHaveBeenCalled();
  });

  it("remember the key for the next action in the same tab", async () => {
    fetchMock.mockResolvedValueOnce(reply(401, {}));
    const user = await openTools();
    await user.click(screen.getByRole("button", { name: "Delete" }));
    await user.click(screen.getByRole("button", { name: "Yes, delete" }));
    fetchMock.mockResolvedValueOnce(reply(200, {}));
    await user.type(await screen.findByLabelText("Admin key"), "s3cret");
    await user.click(screen.getByRole("button", { name: "Continue" }));
    await waitFor(() => expect(screen.getByRole("status")).toHaveTextContent("Page deleted."));
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(callAt(1).key).toBe("s3cret");
  });

  it("show the server's message when a delete fails", async () => {
    fetchMock.mockResolvedValue(reply(404, { error: "No indexed page has this URL." }));
    const user = await openTools();
    await user.click(screen.getByRole("button", { name: "Delete" }));
    await user.click(screen.getByRole("button", { name: "Yes, delete" }));

    await waitFor(() => expect(screen.getByRole("status")).toHaveTextContent("No indexed page has this URL."));
    expect(screen.getByRole("button", { name: "Delete" })).toBeInTheDocument();
  });

  it("set a bid only for a whole number", async () => {
    fetchMock.mockResolvedValue(reply(200, { bid: 7 }));
    const user = await openTools();
    const setBid = screen.getByRole("button", { name: "Set bid" });
    const bid = screen.getByRole("spinbutton");
    expect(setBid).toBeDisabled();

    await user.type(bid, "-3");
    expect(setBid).toBeDisabled();
    await user.clear(bid);
    await user.type(bid, "7");
    expect(setBid).toBeEnabled();
    await user.click(setBid);

    await waitFor(() => expect(screen.getByRole("status")).toHaveTextContent("Bid set to 7."));
    const call = callAt(0);
    expect(call).toMatchObject({ path: "/api/bid", method: "POST" });
    expect(call.params.get("amount")).toBe("7");
    expect(call.params.get("url")).toBe(URL_);
  });

  it("report that the engine could not be reached", async () => {
    fetchMock.mockRejectedValue(new Error("offline"));
    const user = await openTools();
    await user.click(screen.getByRole("button", { name: "Delete" }));
    await user.click(screen.getByRole("button", { name: "Yes, delete" }));
    await waitFor(() =>
      expect(screen.getByRole("status")).toHaveTextContent("The search engine could not be reached."),
    );
  });
});
