export const API_BASE =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

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
  params: { offset?: number; limit?: number; q?: string; sort?: string } = {},
): Promise<ListResult<T>> {
  const u = new URLSearchParams();
  u.set("offset", String(params.offset ?? 0));
  u.set("limit", String(params.limit ?? 30));
  if (params.q) u.set("q", params.q);
  if (params.sort) u.set("sort", params.sort);
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
