"use client";

import * as React from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { toast } from "sonner";
import { ArrowLeft, Pencil, Trash2, Plus, CheckSquare, Phone, CalendarDays, ArrowRightLeft } from "lucide-react";
import { API_BASE, getToken } from "@/lib/api";
import { useDelete, useLookups, useRecord, useUpdate } from "@/lib/hooks";
import { useAuth } from "@/lib/auth";
import { MODULES, RELATED_TO_MODULES, type DetailField } from "@/lib/modules";
import { money } from "@/lib/format";
import { Button, Card, Dialog, InitialChip, StageProgress, Tabs, Badge } from "@/components/ui";
import { RecordDialog } from "@/components/record-dialog";
import { Timeline } from "@/components/timeline";
import { LeadConvertDialog } from "@/components/lead-convert-dialog";

const ACTIVITY_QUICK: { key: string; label: string; icon: React.ComponentType<{ size?: number }> }[] = [
  { key: "tasks", label: "Task", icon: CheckSquare },
  { key: "calls", label: "Log call", icon: Phone },
  { key: "meetings", label: "Meeting", icon: CalendarDays },
];

function fmt(v: any, type?: string) {
  if (v == null || v === "") return "—";
  if (type === "currency") return money(Number(v));
  if (type === "datetime") return String(v).slice(0, 16).replace("T", " ");
  return String(v);
}
const isMono = (t?: string) => t === "currency" || t === "number" || t === "date" || t === "datetime";

export function RecordDetail({ moduleKey, id }: { moduleKey: string; id: string }) {
  const module = MODULES[moduleKey];
  const router = useRouter();
  const { has } = useAuth();
  const { data: record, isLoading } = useRecord(module.endpoint, id);
  const del = useDelete(module.endpoint);
  const update = useUpdate(module.endpoint);
  const [tab, setTab] = React.useState("overview");
  const [edit, setEdit] = React.useState(false);
  const [confirmDel, setConfirmDel] = React.useState(false);
  const [activityKey, setActivityKey] = React.useState<string | null>(null);
  const [convertOpen, setConvertOpen] = React.useState(false);
  const stageLookups = useLookups(module.stageProgress?.lookupCategory);
  const stages = (stageLookups.data ?? []).map((l) => l.label);

  if (isLoading || !record) return <div className="text-muted text-[14px]">Loading…</div>;

  const r = record as any;
  const tabs = [
    { key: "overview", label: "Overview" },
    { key: "timeline", label: "Timeline" },
    ...(module.related.length ? [{ key: "related", label: "Related" }] : []),
  ];

  const doDelete = async () => {
    try { await del.mutateAsync(r.id); toast.success(`${module.singular} deleted`); router.push(`/${module.key}`); }
    catch { toast.error("Couldn't delete — try again."); }
  };

  const canEditStatus = module.stageProgress && has(`${module.perm}_EDIT`);
  const onStatusSelect = (label: string) => {
    const lk = (stageLookups.data ?? []).find((l) => l.label === label);
    if (!lk) return;
    update.mutate({ id: r.id, body: { [module.stageProgress!.idField]: lk.id } }, {
      onSuccess: () => toast.success(`${module.singular} updated`),
      onError: () => toast.error("Couldn't update — try again."),
    });
  };

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center gap-3">
        <Link href={`/${module.key}`}><Button variant="ghost" size="icon" aria-label="Back"><ArrowLeft size={18} /></Button></Link>
        <InitialChip name={module.title(r)} size={36} square={module.key === "accounts"} />
        <div>
          <h1 className="text-[22px] leading-tight">{module.title(r)}</h1>
          {module.subtitle && <p className="text-[13px] text-muted">{module.subtitle(r)}</p>}
        </div>
        <div className="ml-auto flex items-center gap-2">
          {RELATED_TO_MODULES.includes(moduleKey) && ACTIVITY_QUICK.map((a) =>
            has(`${MODULES[a.key].perm}_CREATE`) ? (
              <Button key={a.key} variant="ghost" size="sm" onClick={() => setActivityKey(a.key)}>
                <a.icon size={15} /> {a.label}
              </Button>
            ) : null,
          )}
          {moduleKey === "leads" && !r.converted && has("LEAD_EDIT") && (
            <Button variant="primary" onClick={() => setConvertOpen(true)}><ArrowRightLeft size={15} /> Convert</Button>
          )}
          {has(`${module.perm}_EDIT`) && !(moduleKey === "leads" && r.converted) && (
            <Button variant="secondary" onClick={() => setEdit(true)}><Pencil size={15} /> Edit</Button>
          )}
          {has(`${module.perm}_DELETE`) && <Button variant="ghost" size="icon" aria-label="Delete" onClick={() => setConfirmDel(true)}><Trash2 size={17} /></Button>}
        </div>
      </div>

      {moduleKey === "leads" && r.converted && (
        <Card className="px-5 py-3 flex flex-wrap items-center gap-x-4 gap-y-1">
          <Badge tone="success">Converted</Badge>
          <span className="text-[13px] text-muted">This lead is read-only. Created:</span>
          {r.convertedAccountId && <Link href={`/accounts/${r.convertedAccountId}`} className="text-[13px] font-medium text-accent hover:underline">Account</Link>}
          {r.convertedContactId && <Link href={`/contacts/${r.convertedContactId}`} className="text-[13px] font-medium text-accent hover:underline">Contact</Link>}
          {r.convertedDealId && <Link href={`/deals/${r.convertedDealId}`} className="text-[13px] font-medium text-accent hover:underline">Deal</Link>}
        </Card>
      )}

      <Card className="px-5 py-4">
        <div className="flex flex-wrap gap-x-10 gap-y-3">
          {module.summaryChips.map((c) => (
            <div key={c.key}>
              <p className="eyebrow">{c.label}</p>
              <p className={"text-[15px] font-medium mt-0.5 " + (isMono(c.type) ? "tabular" : "")}>{fmt(r[c.key], c.type)}</p>
            </div>
          ))}
        </div>
        {module.stageProgress && stages.length > 0 && (
          <div className="mt-4 pt-4 border-t border-subtle">
            <StageProgress stages={stages} current={r[module.stageProgress.field]} onSelect={canEditStatus ? onStatusSelect : undefined} />
          </div>
        )}
      </Card>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_320px] gap-4 items-start">
        <Card className="p-0 overflow-hidden">
          <div className="px-5"><Tabs tabs={tabs} value={tab} onChange={setTab} /></div>
          <div className="p-5">
            {tab === "overview" && (
              <div className="flex flex-col gap-6">
                {module.sections.filter((s) => s.fields.length).map((s) => {
                  const present = s.fields.filter((f) => r[f.key] != null && r[f.key] !== "");
                  if (present.length === 0) return null;
                  return (
                    <section key={s.title}>
                      <p className="eyebrow mb-2">{s.title}</p>
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-8 gap-y-2.5">
                        {present.map((f: DetailField) => (
                          <div key={f.key} className="flex justify-between gap-4 border-b border-subtle py-1.5">
                            <span className="text-[13px] text-muted">{f.label}</span>
                            <span className={"text-[13px] text-fg text-right " + (isMono(f.type) ? "tabular" : "")}>{fmt(r[f.key], f.type)}</span>
                          </div>
                        ))}
                      </div>
                    </section>
                  );
                })}
              </div>
            )}
            {tab === "timeline" && <Timeline moduleKey={moduleKey} recordId={r.id} />}
            {tab === "related" && <RelatedLists moduleKey={moduleKey} record={r} />}
          </div>
        </Card>

        <Card className="p-4">
          <p className="eyebrow mb-2">Insights</p>
          <div className="flex flex-col gap-3 text-[13px]">
            <div className="flex justify-between"><span className="text-muted">Owner</span><span className="inline-flex items-center gap-2"><InitialChip name={r.ownerName} size={20} />{r.ownerName}</span></div>
            <div className="flex justify-between"><span className="text-muted">Created</span><span className="tabular">{String(r.createdAt).slice(0, 10)}</span></div>
            <div className="flex justify-between"><span className="text-muted">Updated</span><span className="tabular">{String(r.updatedAt).slice(0, 10)}</span></div>
            {module.key === "deals" && r.expectedRevenue != null && (
              <div className="mt-1 rounded-[10px] bg-accent-soft text-accent-soft-fg px-3 py-2">
                <p className="eyebrow">Expected revenue</p><p className="tabular text-[16px] font-semibold">{money(r.expectedRevenue)}</p>
              </div>
            )}
          </div>
        </Card>
      </div>

      <RecordDialog module={module} record={edit ? r : null} open={edit} onClose={() => setEdit(false)} />

      {activityKey && (
        <RecordDialog
          module={MODULES[activityKey]}
          open={!!activityKey}
          onClose={() => setActivityKey(null)}
          defaults={{ whatType: moduleKey, whatId: r.id }}
        />
      )}

      {moduleKey === "leads" && (
        <LeadConvertDialog lead={r} open={convertOpen} onClose={() => setConvertOpen(false)} />
      )}

      <Dialog open={confirmDel} onClose={() => setConfirmDel(false)} title={`Delete ${module.singular.toLowerCase()}?`}
        footer={<><Button variant="ghost" onClick={() => setConfirmDel(false)}>Cancel</Button><Button variant="danger" onClick={doDelete}>Delete</Button></>}>
        <p className="text-[14px] text-secondary">This can&apos;t be undone. {module.title(r)} will be removed.</p>
      </Dialog>
    </div>
  );
}

function RelatedLists({ moduleKey, record }: { moduleKey: string; record: any }) {
  const module = MODULES[moduleKey];
  return (
    <div className="flex flex-col gap-5">
      {module.related.map((rel) => <RelatedList key={rel.endpoint} rel={rel} parentId={record.id} />)}
    </div>
  );
}

function RelatedList({ rel, parentId }: { rel: { label: string; endpoint: string; foreignKey: string }; parentId: number }) {
  const { data } = useQuery({
    queryKey: ["related", rel.endpoint, rel.foreignKey, parentId],
    queryFn: async () => {
      const res = await fetch(`${API_BASE}/api/${rel.endpoint}?limit=100`, { headers: { Authorization: `Bearer ${getToken()}` } });
      return (res.ok ? res.json() : []) as Promise<any[]>;
    },
  });
  const items = (data ?? []).filter((r) => r[rel.foreignKey] === parentId);
  const m = MODULES[rel.endpoint];
  return (
    <section>
      <p className="eyebrow mb-2">{rel.label} <span className="text-muted">({items.length})</span></p>
      {items.length === 0 ? <p className="text-[13px] text-muted">No {rel.label.toLowerCase()} yet.</p> : (
        <div className="flex flex-col gap-1">
          {items.slice(0, 8).map((r) => (
            <Link key={r.id} href={`/${rel.endpoint}/${r.id}`} className="flex items-center gap-2 py-1.5 px-2 rounded-[8px] hover:bg-hover text-[13px]">
              <InitialChip name={m.title(r)} size={22} square={rel.endpoint === "accounts"} />
              <span className="font-medium text-fg">{m.title(r)}</span>
              {r.amount != null && <span className="ml-auto tabular text-muted">{money(r.amount)}</span>}
            </Link>
          ))}
        </div>
      )}
    </section>
  );
}
