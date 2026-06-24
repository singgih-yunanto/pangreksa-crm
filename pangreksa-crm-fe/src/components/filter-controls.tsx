"use client";

import * as React from "react";
import { useQuery } from "@tanstack/react-query";
import { SlidersHorizontal, X } from "lucide-react";
import { useLookups } from "@/lib/hooks";
import { listRecords } from "@/lib/api";
import type { FilterDef, ModuleConfig } from "@/lib/modules";
import { cn } from "@/lib/cn";
import { Button, Select, Label, Input, Badge } from "@/components/ui";

export type Filters = Record<string, string>;
type OwnerOption = { id: number; fullName: string };

/** The API param keys a given filter contributes to the query string. */
function paramKeys(def: FilterDef): string[] {
  if (def.kind === "numberRange") return [`${def.key}Min`, `${def.key}Max`];
  if (def.kind === "dateRange") return [`${def.key}From`, `${def.key}To`];
  return [def.key];
}

export function countActiveFilters(module: ModuleConfig, filters: Filters): number {
  return (module.filters ?? []).filter((d) => paramKeys(d).some((k) => filters[k])).length;
}

/** Owner dropdown options — only fetched when the user can list users (admin). */
function useOwnerOptions(enabled: boolean) {
  return useQuery({
    queryKey: ["owner-options"],
    enabled,
    staleTime: 5 * 60_000,
    queryFn: async () => (await listRecords<OwnerOption>("users", { limit: 500 })).items,
  });
}

function LookupField({ def, draft, set }: { def: FilterDef; draft: Filters; set: (k: string, v: string) => void }) {
  const lookups = useLookups(def.lookupCategory);
  return (
    <div>
      <Label>{def.label}</Label>
      <Select value={draft[def.key] ?? ""} onChange={(e) => set(def.key, e.target.value)}>
        <option value="">Any</option>
        {(lookups.data ?? []).map((l) => <option key={l.id} value={String(l.id)}>{l.label}</option>)}
      </Select>
    </div>
  );
}

function OwnerField({ def, draft, set, options }: { def: FilterDef; draft: Filters; set: (k: string, v: string) => void; options: OwnerOption[] }) {
  return (
    <div>
      <Label>{def.label}</Label>
      <Select value={draft[def.key] ?? ""} onChange={(e) => set(def.key, e.target.value)}>
        <option value="">Any</option>
        {options.map((u) => <option key={u.id} value={String(u.id)}>{u.fullName}</option>)}
      </Select>
    </div>
  );
}

function RangeField({ def, draft, set, type }: { def: FilterDef; draft: Filters; set: (k: string, v: string) => void; type: "number" | "date" }) {
  const [a, b] = paramKeys(def);
  const [aPh, bPh] = type === "number" ? ["Min", "Max"] : ["From", "To"];
  return (
    <div>
      <Label>{def.label}</Label>
      <div className="flex items-center gap-2">
        <Input type={type} placeholder={aPh} value={draft[a] ?? ""} onChange={(e) => set(a, e.target.value)} />
        <span className="text-muted text-[13px]">–</span>
        <Input type={type} placeholder={bPh} value={draft[b] ?? ""} onChange={(e) => set(b, e.target.value)} />
      </div>
    </div>
  );
}

/** Filter button + popover panel. `value` is committed filters; `onChange` applies a new set. */
export function FilterPopover({ module, value, onChange, canOwner }: {
  module: ModuleConfig; value: Filters; onChange: (f: Filters) => void; canOwner: boolean;
}) {
  const defs = module.filters ?? [];
  const [open, setOpen] = React.useState(false);
  const [draft, setDraft] = React.useState<Filters>(value);
  const ref = React.useRef<HTMLDivElement>(null);
  const owners = useOwnerOptions(canOwner && defs.some((d) => d.kind === "owner"));

  React.useEffect(() => { if (open) setDraft(value); }, [open, value]);
  React.useEffect(() => {
    const onClick = (e: MouseEvent) => { if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false); };
    document.addEventListener("mousedown", onClick);
    return () => document.removeEventListener("mousedown", onClick);
  }, []);

  const set = (k: string, v: string) => setDraft((d) => {
    const next = { ...d };
    if (v) next[k] = v; else delete next[k];
    return next;
  });
  const count = countActiveFilters(module, value);
  const visible = defs.filter((d) => d.kind !== "owner" || canOwner);

  return (
    <div className="relative" ref={ref}>
      <Button variant="secondary" onClick={() => setOpen((o) => !o)}>
        <SlidersHorizontal size={15} /> Filter
        {count > 0 && <Badge tone="accent" className="ml-1">{count}</Badge>}
      </Button>
      {open && (
        <div className="absolute right-0 top-11 z-40 w-80 bg-card border border-subtle rounded-[12px] shadow-[var(--shadow-md)] p-3"
          style={{ animation: "pgz-in 140ms cubic-bezier(0.16,1,0.3,1)" }}>
          <div className="flex flex-col gap-3 max-h-[60vh] overflow-y-auto pr-0.5">
            {visible.map((def) => {
              if (def.kind === "lookup") return <LookupField key={def.key} def={def} draft={draft} set={set} />;
              if (def.kind === "owner") return <OwnerField key={def.key} def={def} draft={draft} set={set} options={owners.data ?? []} />;
              return <RangeField key={def.key} def={def} draft={draft} set={set} type={def.kind === "numberRange" ? "number" : "date"} />;
            })}
          </div>
          <div className="flex items-center justify-between gap-2 mt-3 pt-3 border-t border-subtle">
            <Button variant="ghost" size="sm" onClick={() => { setDraft({}); onChange({}); setOpen(false); }}>Clear all</Button>
            <Button variant="primary" size="sm" onClick={() => { onChange(draft); setOpen(false); }}>Apply</Button>
          </div>
        </div>
      )}
    </div>
  );
}

/* ----------------------------- Active filter chips ----------------------------- */
function ChipShell({ label, onClear }: { label: string; onClear: () => void }) {
  return (
    <span className="inline-flex items-center gap-1 rounded-full border border-line bg-card px-2.5 py-1 text-[12px]">
      <span className="text-secondary">{label}</span>
      <button onClick={onClear} aria-label={`Clear ${label}`} className="text-muted hover:text-fg cursor-pointer"><X size={13} /></button>
    </span>
  );
}

function LookupChip({ def, id, onClear }: { def: FilterDef; id: string; onClear: () => void }) {
  const lookups = useLookups(def.lookupCategory);
  const label = (lookups.data ?? []).find((l) => String(l.id) === id)?.label ?? id;
  return <ChipShell label={`${def.label}: ${label}`} onClear={onClear} />;
}

function OwnerChip({ def, id, onClear, options }: { def: FilterDef; id: string; onClear: () => void; options: OwnerOption[] }) {
  const label = options.find((u) => String(u.id) === id)?.fullName ?? id;
  return <ChipShell label={`${def.label}: ${label}`} onClear={onClear} />;
}

export function FilterChips({ module, value, onChange, canOwner }: {
  module: ModuleConfig; value: Filters; onChange: (f: Filters) => void; canOwner: boolean;
}) {
  const defs = module.filters ?? [];
  const owners = useOwnerOptions(canOwner && defs.some((d) => d.kind === "owner"));
  const clear = (keys: string[]) => { const next = { ...value }; keys.forEach((k) => delete next[k]); onChange(next); };

  const active = defs.filter((d) => paramKeys(d).some((k) => value[k]));
  if (active.length === 0) return null;

  return (
    <div className="flex items-center gap-1.5 flex-wrap">
      {active.map((def) => {
        const keys = paramKeys(def);
        if (def.kind === "lookup") return <LookupChip key={def.key} def={def} id={value[def.key]} onClear={() => clear(keys)} />;
        if (def.kind === "owner") return <OwnerChip key={def.key} def={def} id={value[def.key]} onClear={() => clear(keys)} options={owners.data ?? []} />;
        const [a, b] = keys;
        const lo = value[a]; const hi = value[b];
        const txt = `${lo ?? "…"} – ${hi ?? "…"}`;
        return <ChipShell key={def.key} label={`${def.label}: ${txt}`} onClear={() => clear(keys)} />;
      })}
      <button onClick={() => onChange({})} className="text-[12px] text-link hover:underline cursor-pointer ml-1">Clear all</button>
    </div>
  );
}
