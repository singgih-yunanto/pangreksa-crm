"use client";

import * as React from "react";
import { useSearchParams } from "next/navigation";
import { toast } from "sonner";
import { List, Columns3, CalendarDays, Search, Plus, Download } from "lucide-react";
import { MODULES } from "@/lib/modules";
import { useAuth } from "@/lib/auth";
import { cn } from "@/lib/cn";
import { Button, Input } from "@/components/ui";
import { ListView } from "@/components/list-view";
import { KanbanView } from "@/components/kanban-view";
import { CalendarView } from "@/components/calendar-view";
import { RecordDialog } from "@/components/record-dialog";
import { FilterPopover, FilterChips, type Filters } from "@/components/filter-controls";
import { downloadCsv } from "@/lib/export";

export function ModuleScreen({ moduleKey }: { moduleKey: string }) {
  const module = MODULES[moduleKey];
  const { has } = useAuth();
  const search = useSearchParams();
  const [view, setView] = React.useState<"list" | "kanban" | "calendar">("list");
  const [q, setQ] = React.useState("");
  const [filters, setFilters] = React.useState<Filters>({});
  const [dialog, setDialog] = React.useState(false);
  const [exporting, setExporting] = React.useState(false);

  React.useEffect(() => { if (search.get("new")) setDialog(true); }, [search]);

  const canOwnerFilter = has("ADMIN_USERS");
  const onExport = async () => {
    setExporting(true);
    try {
      const n = await downloadCsv(module, q, filters);
      toast.success(`Exported ${n} ${module.plural.toLowerCase()}.`);
    } catch {
      toast.error("Export failed — try again.");
    } finally {
      setExporting(false);
    }
  };

  return (
    <div className="flex flex-col gap-4 h-[calc(100vh-6.5rem)]">
      <div className="flex items-center gap-3 flex-wrap">
        <div>
          <p className="eyebrow">{module.plural}</p>
          <h1 className="text-[26px] leading-tight">{module.plural}</h1>
        </div>
        <div className="ml-auto flex items-center gap-2">
          <div className="relative">
            <Search size={15} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-muted" />
            <Input placeholder={`Search ${module.plural.toLowerCase()}`} value={q} onChange={(e) => setQ(e.target.value)} className="pl-8 w-56" />
          </div>
          {module.filters && module.filters.length > 0 && (
            <FilterPopover module={module} value={filters} onChange={setFilters} canOwner={canOwnerFilter} />
          )}
          <Button variant="secondary" onClick={onExport} disabled={exporting} aria-label="Download CSV">
            <Download size={15} /> {exporting ? "Exporting…" : "Export"}
          </Button>
          {(module.kanban || module.calendar) && (
            <div className="flex items-center rounded-[8px] border border-line bg-card p-0.5">
              <button onClick={() => setView("list")} aria-label="List view"
                className={cn("h-7 w-8 inline-flex items-center justify-center rounded-[6px] cursor-pointer", view === "list" ? "bg-accent-soft text-accent-soft-fg" : "text-muted hover:text-fg")}>
                <List size={16} />
              </button>
              {module.kanban && (
                <button onClick={() => setView("kanban")} aria-label="Kanban view"
                  className={cn("h-7 w-8 inline-flex items-center justify-center rounded-[6px] cursor-pointer", view === "kanban" ? "bg-accent-soft text-accent-soft-fg" : "text-muted hover:text-fg")}>
                  <Columns3 size={16} />
                </button>
              )}
              {module.calendar && (
                <button onClick={() => setView("calendar")} aria-label="Calendar view"
                  className={cn("h-7 w-8 inline-flex items-center justify-center rounded-[6px] cursor-pointer", view === "calendar" ? "bg-accent-soft text-accent-soft-fg" : "text-muted hover:text-fg")}>
                  <CalendarDays size={16} />
                </button>
              )}
            </div>
          )}
          {has(`${module.perm}_CREATE`) && <Button variant="primary" onClick={() => setDialog(true)}><Plus size={16} /> New {module.singular.toLowerCase()}</Button>}
        </div>
      </div>

      {module.filters && <FilterChips module={module} value={filters} onChange={setFilters} canOwner={canOwnerFilter} />}

      <div className="flex-1 min-h-0">
        {view === "kanban" && module.kanban ? <KanbanView module={module} />
          : view === "calendar" && module.calendar ? <CalendarView module={module} />
          : <ListView module={module} q={q} filters={filters} />}
      </div>

      <RecordDialog module={module} open={dialog} onClose={() => setDialog(false)} />
    </div>
  );
}
