"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { useAllRecords } from "@/lib/hooks";
import type { ModuleConfig } from "@/lib/modules";
import { cn } from "@/lib/cn";
import { Button, Card } from "@/components/ui";

const DOW = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
const MONTHS = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];

const ymd = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
const dayKey = (v: unknown) => (v == null ? null : String(v).slice(0, 10));

export function CalendarView({ module }: { module: ModuleConfig }) {
  const router = useRouter();
  const dateField = module.calendar!.dateField;
  const { data, isLoading } = useAllRecords(module.endpoint);
  const [month, setMonth] = React.useState(() => { const t = new Date(); return new Date(t.getFullYear(), t.getMonth(), 1); });

  const byDay = React.useMemo(() => {
    const map = new Map<string, any[]>();
    for (const r of data ?? []) {
      const k = dayKey((r as any)[dateField]);
      if (!k) continue;
      (map.get(k) ?? map.set(k, []).get(k)!).push(r);
    }
    return map;
  }, [data, dateField]);

  // Grid starts on the Monday on/before the 1st and runs 6 weeks (42 cells).
  const first = new Date(month.getFullYear(), month.getMonth(), 1);
  const offset = (first.getDay() + 6) % 7; // days since Monday
  const start = new Date(first);
  start.setDate(first.getDate() - offset);
  const cells = Array.from({ length: 42 }, (_, i) => { const d = new Date(start); d.setDate(start.getDate() + i); return d; });
  const todayKey = ymd(new Date());

  return (
    <Card className="flex flex-col h-full overflow-hidden">
      <div className="flex items-center gap-3 px-4 h-12 border-b border-subtle">
        <h2 className="text-[15px] font-semibold">{MONTHS[month.getMonth()]} {month.getFullYear()}</h2>
        <div className="ml-auto flex items-center gap-1">
          <Button variant="ghost" size="icon" aria-label="Previous month" onClick={() => setMonth(new Date(month.getFullYear(), month.getMonth() - 1, 1))}><ChevronLeft size={18} /></Button>
          <Button variant="secondary" size="sm" onClick={() => { const t = new Date(); setMonth(new Date(t.getFullYear(), t.getMonth(), 1)); }}>Today</Button>
          <Button variant="ghost" size="icon" aria-label="Next month" onClick={() => setMonth(new Date(month.getFullYear(), month.getMonth() + 1, 1))}><ChevronRight size={18} /></Button>
        </div>
      </div>
      <div className="grid grid-cols-7 border-b border-subtle">
        {DOW.map((d) => <div key={d} className="eyebrow px-2 py-1.5 text-center">{d}</div>)}
      </div>
      <div className="grid grid-cols-7 flex-1 min-h-0 overflow-auto">
        {cells.map((d, i) => {
          const key = ymd(d);
          const items = byDay.get(key) ?? [];
          const inMonth = d.getMonth() === month.getMonth();
          return (
            <div key={i} className={cn("min-h-24 border-b border-r border-subtle p-1.5 flex flex-col gap-1", !inMonth && "bg-sunken/40")}>
              <span className={cn("text-[12px] tabular self-end w-6 h-6 inline-flex items-center justify-center rounded-full",
                key === todayKey ? "bg-accent text-on-accent font-semibold" : inMonth ? "text-secondary" : "text-muted")}>
                {d.getDate()}
              </span>
              {items.slice(0, 4).map((r: any) => (
                <button key={r.id} onClick={() => router.push(`/${module.key}/${r.id}`)}
                  className="text-left text-[12px] truncate rounded-[6px] px-1.5 py-0.5 bg-accent-soft text-accent-soft-fg hover:opacity-90 cursor-pointer">
                  {module.title(r)}
                </button>
              ))}
              {items.length > 4 && <span className="text-[11px] text-muted px-1">+{items.length - 4} more</span>}
            </div>
          );
        })}
      </div>
      {isLoading && <div className="px-4 py-2 text-[12px] text-muted">Loading…</div>}
    </Card>
  );
}
