"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import {
  DndContext, PointerSensor, useDraggable, useDroppable, useSensor, useSensors, type DragEndEvent,
} from "@dnd-kit/core";
import { toast } from "sonner";
import { useAllRecords, useLookups, useUpdate } from "@/lib/hooks";
import type { ModuleConfig } from "@/lib/modules";
import { money } from "@/lib/format";
import { cn } from "@/lib/cn";
import { InitialChip } from "@/components/ui";

function KCard({ module, row }: { module: ModuleConfig; row: any }) {
  const router = useRouter();
  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({ id: String(row.id), data: { row } });
  return (
    <div ref={setNodeRef} {...attributes} {...listeners}
      onClick={() => router.push(`/${module.key}/${row.id}`)}
      style={{ transform: transform ? `translate(${transform.x}px, ${transform.y}px)` : undefined }}
      className={cn(
        "bg-card border-l-2 border-accent border border-subtle rounded-[10px] p-3 cursor-grab active:cursor-grabbing shadow-[var(--shadow-sm)]",
        isDragging && "opacity-50",
      )}>
      <p className="font-medium text-[14px] leading-snug">{module.title(row)}</p>
      {module.subtitle && <p className="text-[12px] text-muted mt-0.5">{module.subtitle(row)}</p>}
      <div className="flex items-center justify-between mt-2">
        {row.amount != null ? <span className="tabular text-[13px] font-medium">{money(row.amount)}{row.probability != null && <span className="text-muted"> · {row.probability}%</span>}</span> : <span />}
        <InitialChip name={row.ownerName} size={20} />
      </div>
    </div>
  );
}

function KColumn({ id, label, count, sum, children }: { id: string; label: string; count: number; sum?: number; children: React.ReactNode }) {
  const { setNodeRef, isOver } = useDroppable({ id });
  return (
    <div className="w-72 shrink-0 flex flex-col">
      <div className="flex items-center gap-2 px-1 pb-2">
        <span className="font-medium text-[13px]">{label}</span>
        <span className="text-[12px] text-muted tabular">{count}</span>
        {sum != null && <span className="ml-auto tabular text-[12px] text-muted">{money(sum)}</span>}
      </div>
      <div ref={setNodeRef} className={cn("flex-1 min-h-24 rounded-[12px] p-2 flex flex-col gap-2 bg-sunken/60 border border-dashed", isOver ? "border-accent" : "border-transparent")}>
        {children}
      </div>
    </div>
  );
}

export function KanbanView({ module }: { module: ModuleConfig }) {
  const { field, idField, lookupCategory, sumField } = module.kanban!;
  const lookups = useLookups(lookupCategory);
  const { data } = useAllRecords(module.endpoint);
  const update = useUpdate(module.endpoint);
  const [rows, setRows] = React.useState<any[]>([]);
  React.useEffect(() => { if (data) setRows(data); }, [data]);
  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 6 } }));

  const cols = (lookups.data ?? []).map((l) => ({ label: l.label, id: l.id }));

  const onDragEnd = (e: DragEndEvent) => {
    const id = e.active.id;
    const targetLabel = e.over?.id as string | undefined;
    if (!targetLabel) return;
    const row = rows.find((r) => String(r.id) === String(id));
    if (!row || (row[field] ?? "") === targetLabel) return;
    const targetLookup = (lookups.data ?? []).find((l) => l.label === targetLabel);
    if (!targetLookup) return;
    const prev = row[field];
    setRows((rs) => rs.map((r) => (String(r.id) === String(id) ? { ...r, [field]: targetLabel } : r)));
    update.mutate({ id: row.id, body: { [idField]: targetLookup.id } }, {
      onSuccess: () => toast.success(`${module.singular} moved to ${targetLabel}`),
      onError: () => { toast.error("Couldn't move — try again."); setRows((rs) => rs.map((r) => (String(r.id) === String(id) ? { ...r, [field]: prev } : r))); },
    });
  };

  return (
    <DndContext sensors={sensors} onDragEnd={onDragEnd}>
      <div className="flex gap-3 overflow-x-auto pb-3">
        {cols.map((col) => {
          const items = rows.filter((r) => (r[field] ?? "") === col.label);
          const sum = sumField ? items.reduce((n, r) => n + (Number(r[sumField]) || 0), 0) : undefined;
          return (
            <KColumn key={col.label} id={col.label} label={col.label} count={items.length} sum={sum}>
              {items.map((r) => <KCard key={r.id} module={module} row={r} />)}
            </KColumn>
          );
        })}
      </div>
    </DndContext>
  );
}
