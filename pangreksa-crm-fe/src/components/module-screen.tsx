"use client";

import * as React from "react";
import { useSearchParams } from "next/navigation";
import { List, Columns3, Search, Plus } from "lucide-react";
import { MODULES } from "@/lib/modules";
import { useAuth } from "@/lib/auth";
import { cn } from "@/lib/cn";
import { Button, Input } from "@/components/ui";
import { ListView } from "@/components/list-view";
import { KanbanView } from "@/components/kanban-view";
import { RecordDialog } from "@/components/record-dialog";

export function ModuleScreen({ moduleKey }: { moduleKey: string }) {
  const module = MODULES[moduleKey];
  const { has } = useAuth();
  const search = useSearchParams();
  const [view, setView] = React.useState<"list" | "kanban">(module.kanban ? "list" : "list");
  const [q, setQ] = React.useState("");
  const [dialog, setDialog] = React.useState(false);

  React.useEffect(() => { if (search.get("new")) setDialog(true); }, [search]);

  return (
    <div className="flex flex-col gap-4">
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
          {module.kanban && (
            <div className="flex items-center rounded-[8px] border border-line bg-card p-0.5">
              <button onClick={() => setView("list")} aria-label="List view"
                className={cn("h-7 w-8 inline-flex items-center justify-center rounded-[6px] cursor-pointer", view === "list" ? "bg-accent-soft text-accent-soft-fg" : "text-muted hover:text-fg")}>
                <List size={16} />
              </button>
              <button onClick={() => setView("kanban")} aria-label="Kanban view"
                className={cn("h-7 w-8 inline-flex items-center justify-center rounded-[6px] cursor-pointer", view === "kanban" ? "bg-accent-soft text-accent-soft-fg" : "text-muted hover:text-fg")}>
                <Columns3 size={16} />
              </button>
            </div>
          )}
          {has(`${module.perm}_CREATE`) && <Button variant="primary" onClick={() => setDialog(true)}><Plus size={16} /> New {module.singular.toLowerCase()}</Button>}
        </div>
      </div>

      {view === "kanban" && module.kanban ? <KanbanView module={module} /> : <ListView module={module} q={q} />}

      <RecordDialog module={module} open={dialog} onClose={() => setDialog(false)} />
    </div>
  );
}
