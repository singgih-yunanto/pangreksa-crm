import { listRecords, type Record } from "@/lib/api";
import type { ModuleConfig } from "@/lib/modules";

/** Page through the list API (forwarding search + filters) to collect every matching row. */
async function fetchAllMatching(
  endpoint: string,
  q: string,
  filters: { [k: string]: string },
): Promise<Record[]> {
  const out: Record[] = [];
  let offset = 0;
  for (;;) {
    const { items, total } = await listRecords(endpoint, { offset, limit: 100, q, filters });
    out.push(...items);
    offset += items.length;
    if (items.length === 0 || out.length >= total) break;
  }
  return out;
}

function csvCell(v: unknown): string {
  if (v == null) return "";
  const s = String(v);
  return /[",\n\r]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
}

/** Download an array of flat objects as a CSV (used by report cards). Columns = keys of the first row. */
export function downloadRowsCsv(filename: string, rows: { [k: string]: unknown }[]): void {
  const cols = rows.length ? Object.keys(rows[0]) : [];
  const header = cols.map(csvCell).join(",");
  const body = rows.map((r) => cols.map((c) => csvCell(r[c])).join(","));
  const csv = [header, ...body].join("\r\n");
  const blob = new Blob(["﻿" + csv], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `${filename}-${new Date().toISOString().slice(0, 10)}.csv`;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

/** Download the module's list (respecting current search + filters) as a CSV (Excel-friendly). */
export async function downloadCsv(
  module: ModuleConfig,
  q: string,
  filters: { [k: string]: string },
): Promise<number> {
  const rows = await fetchAllMatching(module.endpoint, q, filters);
  const cols = module.columns;
  const header = cols.map((c) => csvCell(c.label)).join(",");
  const body = rows.map((r) => cols.map((c) => csvCell((r as Record)[c.key])).join(","));
  const csv = [header, ...body].join("\r\n");
  // Prepend a BOM so Excel detects UTF-8 (Indonesian names, currency, etc.).
  const blob = new Blob(["﻿" + csv], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `${module.plural}-${new Date().toISOString().slice(0, 10)}.csv`;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
  return rows.length;
}
