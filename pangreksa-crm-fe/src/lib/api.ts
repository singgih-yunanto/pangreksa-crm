// Same-origin in production: when NEXT_PUBLIC_API_BASE_URL is unset/empty, the app calls
// "/api/..." RELATIVE, so nginx serves the frontend and proxies the API on one origin (no
// CORS). Only `next dev` (no nginx in front) falls back to the backend on localhost:8080.
export const API_BASE =
  process.env.NEXT_PUBLIC_API_BASE_URL ||
  (process.env.NODE_ENV === "development" ? "http://localhost:8080" : "");

const TOKEN_KEY = "pangreksa-token";
export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY);
}
export function setToken(t: string) { localStorage.setItem(TOKEN_KEY, t); }
export function clearToken() { localStorage.removeItem(TOKEN_KEY); }

export type Record = { id: number; [k: string]: unknown };
export type ListResult<T> = { items: T[]; total: number };
export type FieldError = { field: string | null; message: string };
export class ApiError extends Error {
  errors: FieldError[];
  status: number;
  constructor(message: string, errors: FieldError[] = [], status = 0) {
    super(message);
    this.errors = errors;
    this.status = status;
  }
}

async function authedFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const token = getToken();
  const headers = new Headers(init.headers);
  if (token) headers.set("Authorization", `Bearer ${token}`);
  const res = await fetch(`${API_BASE}${path}`, { ...init, headers });
  if (res.status === 401) {
    clearToken();
    if (typeof window !== "undefined" && !location.pathname.startsWith("/login")) {
      location.assign("/login");
    }
    throw new ApiError("Your session has expired. Please sign in again.", [], 401);
  }
  return res;
}

async function ok(res: Response): Promise<Response> {
  if (res.ok) return res;
  let body: { message?: string; errors?: FieldError[] } = {};
  try { body = await res.json(); } catch { /* ignore */ }
  throw new ApiError(body.message ?? `Request failed (${res.status})`, body.errors ?? [], res.status);
}

/* ----------------------------- Auth ----------------------------- */
export type Me = { id: number; username: string; fullName: string; roleName: string | null; permissions: string[] };

export async function login(username: string, password: string): Promise<{ token: string; user: Me }> {
  const res = await ok(await fetch(`${API_BASE}/api/auth/login`, {
    method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ username, password }),
  }));
  return res.json();
}
export async function fetchMe(): Promise<Me> {
  return (await ok(await authedFetch("/api/auth/me"))).json();
}

/* ----------------------------- Records ----------------------------- */
export async function listRecords<T = Record>(
  endpoint: string,
  params: { offset?: number; limit?: number; q?: string; sort?: string; filters?: { [k: string]: string } } = {},
): Promise<ListResult<T>> {
  const u = new URLSearchParams();
  u.set("offset", String(params.offset ?? 0));
  u.set("limit", String(params.limit ?? 30));
  if (params.q) u.set("q", params.q);
  if (params.sort) u.set("sort", params.sort);
  for (const [k, v] of Object.entries(params.filters ?? {})) {
    if (v !== "" && v != null) u.set(k, v);
  }
  const res = await ok(await authedFetch(`/api/${endpoint}?${u.toString()}`));
  const total = Number(res.headers.get("X-Total-Count") ?? "0");
  return { items: (await res.json()) as T[], total };
}
export async function getRecord<T = Record>(endpoint: string, id: number | string): Promise<T> {
  return (await ok(await authedFetch(`/api/${endpoint}/${id}`))).json();
}
export async function createRecord<T = Record>(endpoint: string, body: unknown): Promise<T> {
  return (await ok(await authedFetch(`/api/${endpoint}`, {
    method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body),
  }))).json();
}
export async function updateRecord<T = Record>(endpoint: string, id: number | string, body: unknown): Promise<T> {
  return (await ok(await authedFetch(`/api/${endpoint}/${id}`, {
    method: "PATCH", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body),
  }))).json();
}
export async function deleteRecord(endpoint: string, id: number | string): Promise<void> {
  await ok(await authedFetch(`/api/${endpoint}/${id}`, { method: "DELETE" }));
}

/* ----------------------------- Lookups & config ----------------------------- */
export type Lookup = { id: number; category: string; code: string; label: string; sortOrder: number; extra: { [k: string]: unknown } };
export async function fetchLookups(category: string): Promise<Lookup[]> {
  return (await ok(await authedFetch(`/api/lookups?category=${encodeURIComponent(category)}`))).json();
}
export type AppConfig = { currency: string; decimalPlaces: number; locale: string };
export async function fetchConfig(): Promise<AppConfig> {
  return (await ok(await authedFetch(`/api/configuration`))).json();
}

/* ----------------------------- Timeline ----------------------------- */
export type TimelineItem = {
  kind: string; at: string; title: string; subtitle: string | null;
  actor: string | null; refModule: string | null; refId: number | null;
};
export async function fetchTimeline(type: string, id: number | string): Promise<TimelineItem[]> {
  return (await ok(await authedFetch(`/api/timeline?type=${encodeURIComponent(type)}&id=${id}`))).json();
}

/* ----------------------------- Notes ----------------------------- */
export type NoteItem = {
  id: number; parentType: string; parentId: number; body: string;
  authorId: number | null; authorName: string | null; createdAt: string;
};
export async function listNotes(type: string, id: number | string): Promise<NoteItem[]> {
  return (await ok(await authedFetch(`/api/notes?type=${encodeURIComponent(type)}&id=${id}`))).json();
}
export async function addNote(type: string, id: number | string, body: string): Promise<NoteItem> {
  return (await ok(await authedFetch(`/api/notes`, {
    method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ parentType: type, parentId: Number(id), body }),
  }))).json();
}
export async function deleteNote(id: number): Promise<void> {
  await ok(await authedFetch(`/api/notes/${id}`, { method: "DELETE" }));
}

/* ----------------------------- Attachments ----------------------------- */
export type AttachmentItem = {
  id: number; parentType: string; parentId: number; filename: string;
  contentType: string | null; sizeBytes: number; ownerId: number | null; ownerName: string | null; createdAt: string;
};
export async function listAttachments(type: string, id: number | string): Promise<AttachmentItem[]> {
  return (await ok(await authedFetch(`/api/attachments?type=${encodeURIComponent(type)}&id=${id}`))).json();
}
export async function uploadAttachment(type: string, id: number | string, file: File): Promise<AttachmentItem> {
  const fd = new FormData();
  fd.set("parentType", type);
  fd.set("parentId", String(id));
  fd.set("file", file);
  return (await ok(await authedFetch(`/api/attachments`, { method: "POST", body: fd }))).json();
}
export async function deleteAttachment(id: number): Promise<void> {
  await ok(await authedFetch(`/api/attachments/${id}`, { method: "DELETE" }));
}
/** Fetch the blob (with the auth header) and trigger a browser download. */
export async function downloadAttachment(id: number, filename: string): Promise<void> {
  const res = await ok(await authedFetch(`/api/attachments/${id}/download`));
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url; a.download = filename;
  document.body.appendChild(a); a.click(); a.remove();
  URL.revokeObjectURL(url);
}

/* ----------------------------- Lead conversion ----------------------------- */
export type LeadConvertBody = {
  accountId?: number; contactId?: number;
  createDeal?: boolean; dealName?: string; dealStageId?: number; dealAmount?: number; dealClosingDate?: string;
};
export type LeadConversionResult = { leadId: number; accountId: number; contactId: number; dealId: number | null };
export async function convertLead(id: number | string, body: LeadConvertBody): Promise<LeadConversionResult> {
  return (await ok(await authedFetch(`/api/leads/${id}/convert`, {
    method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body),
  }))).json();
}

/* ----------------------------- Analytics ----------------------------- */
export type AnalyticsFilter = { ownerId?: number | ""; from?: string; to?: string };
export type Summary = {
  pipelineValue: number; expectedRevenue: number; winRate: number;
  openDeals: number; wonDeals: number; openLeads: number;
};
export type Bucket = { label: string; count: number; amount?: number | null };
export type OwnerSales = { owner: string; count: number; total: number; won: number; open: number };
export type LeadStats = { byStatus: Bucket[]; converted: number };
export type ActivityStats = { tasks: number; meetings: number; calls: number; tasksByStatus: Bucket[] };

function analyticsQs(f?: AnalyticsFilter): string {
  const u = new URLSearchParams();
  if (f?.ownerId) u.set("ownerId", String(f.ownerId));
  if (f?.from) u.set("from", f.from);
  if (f?.to) u.set("to", f.to);
  const s = u.toString();
  return s ? `?${s}` : "";
}

export async function fetchSummary(f?: AnalyticsFilter): Promise<Summary> {
  return (await ok(await authedFetch(`/api/analytics/summary${analyticsQs(f)}`))).json();
}
export async function fetchPipelineByStage(f?: AnalyticsFilter): Promise<Bucket[]> {
  return (await ok(await authedFetch(`/api/analytics/pipeline-by-stage${analyticsQs(f)}`))).json();
}
export async function fetchSalesByOwner(f?: AnalyticsFilter): Promise<OwnerSales[]> {
  return (await ok(await authedFetch(`/api/analytics/sales-by-owner${analyticsQs(f)}`))).json();
}
export async function fetchLeadsByStatus(f?: AnalyticsFilter): Promise<LeadStats> {
  return (await ok(await authedFetch(`/api/analytics/leads-by-status${analyticsQs(f)}`))).json();
}
export async function fetchActivityStats(f?: AnalyticsFilter): Promise<ActivityStats> {
  return (await ok(await authedFetch(`/api/analytics/activities${analyticsQs(f)}`))).json();
}
