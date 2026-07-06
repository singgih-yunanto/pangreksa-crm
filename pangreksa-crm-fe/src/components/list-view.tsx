"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { useInfiniteList, useLookups, useUpdate } from "@/lib/hooks";
import { useAuth } from "@/lib/auth";
import type { ColumnDef, ModuleConfig } from "@/lib/modules";
import { money } from "@/lib/format";
import { cn } from "@/lib/cn";
import { Card, InitialChip } from "@/components/ui";

const isNumeric = (col: ColumnDef) => col.type === "currency" || col.type === "number";

function cell(row: any, col: ColumnDef) {
  const v = row[col.key];
  if (col.type === "currency") return <span className="tabular">{money(v as number)}</span>;
  if (col.key === "ownerName") return <span className="inline-flex items-center gap-2"><InitialChip name={v as string} size={22} />{v as string}</span>;
  if (col.type === "number") return <span className="tabular">{v ?? "—"}</span>;
  if (col.type === "date") return <span className="tabular">{v ?? "—"}</span>;
  if (col.type === "datetime") return <span className="tabular">{v ? String(v).slice(0, 16).replace("T", " ") : "—"}</span>;
  return <span>{v == null || v === "" ? "—" : String(v)}</span>;
}

/** Inline status editor for the workflow-status column (stageProgress.field). */
function StatusCell({ module, row }: { module: ModuleConfig; row: any }) {
  const sp = module.stageProgress!;
  const lookups = useLookups(sp.lookupCategory);
  const update = useUpdate(module.endpoint);
  const currentId = (lookups.data ?? []).find((l) => l.label === row[sp.field])?.id ?? "";
  return (
    <select
      value={currentId}
      disabled={update.isPending || lookups.isLoading}
      onClick={(e) => e.stopPropagation()}
      onChange={(e) => {
        e.stopPropagation();
        update.mutate({ id: row.id, body: { [sp.idField]: Number(e.target.value) } }, {
          onSuccess: () => toast.success(`${module.singular} updated`),
          onError: () => toast.error("Couldn't update — try again."),
        });
      }}
      className="cursor-pointer appearance-none rounded-[6px] border border-line bg-card px-2 py-1 text-[13px] text-fg hover:border-accent focus:border-accent disabled:opacity-60"
    >
      {(lookups.data ?? []).map((l) => <option key={l.id} value={l.id}>{l.label}</option>)}
    </select>
  );
}

export function ListView({ module, q, filters }: { module: ModuleConfig; q: string; filters?: Record<string, string> }) {
  const router = useRouter();
  const { has } = useAuth();
  const { data, fetchNextPage, hasNextPage, isFetchingNextPage, isLoading } = useInfiniteList(module.endpoint, q, undefined, filters);
  const rows = data?.pages.flatMap((p) => p.items) ?? [];
  const sentinel = React.useRef<HTMLDivElement>(null);
  const statusField = module.stageProgress?.field;
  const canEditStatus = !!module.stageProgress && has(`${module.perm}_EDIT`);

  React.useEffect(() => {
    const el = sentinel.current;
    if (!el) return;
    const io = new IntersectionObserver((e) => {
      if (e[0].isIntersecting && hasNextPage && !isFetchingNextPage) fetchNextPage();
    }, { rootMargin: "200px" });
    io.observe(el);
    return () => io.disconnect();
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  if (!isLoading && rows.length === 0) {
    return (
      <Card className="p-10 text-center">
        <p className="text-[15px] font-medium">No {module.plural.toLowerCase()} found</p>
        <p className="text-[13px] text-muted mt-1">Try adjusting your search or filters, or create a new {module.singular.toLowerCase()}.</p>
      </Card>
    );
  }

  return (
    <Card className="flex flex-col h-full overflow-hidden">
      <div className="flex-1 min-h-0 overflow-auto">
        <table className="w-full text-[14px]">
          <thead className="sticky top-0 z-10 bg-card">
            <tr className="text-left border-b border-subtle">
              {module.columns.map((c) => (
                <th key={c.key} className={cn("eyebrow font-semibold px-4 h-10", isNumeric(c) && "text-right")}>{c.label}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.map((row: any) => (
              <tr key={row.id} onClick={() => router.push(`/${module.key}/${row.id}`)}
                className="border-b border-subtle last:border-0 hover:bg-hover cursor-pointer">
                {module.columns.map((c) => (
                  <td key={c.key} className={cn("px-4 h-12", isNumeric(c) && "text-right", c.primary ? "font-medium text-fg" : "text-secondary")}>
                    {canEditStatus && c.key === statusField ? <StatusCell module={module} row={row} /> : cell(row, c)}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
        <div ref={sentinel} className="h-10 flex items-center justify-center text-[12px] text-muted">
          {isFetchingNextPage ? "Loading more…" : hasNextPage ? "" : isLoading ? "Loading…" : ""}
        </div>
      </div>
    </Card>
  );
}
