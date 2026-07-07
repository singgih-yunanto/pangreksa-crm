"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { fetchPipelineByStage, fetchSummary, listRecords, type Record } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { money } from "@/lib/format";
import { Card, InitialChip } from "@/components/ui";
import { EChart, barOption } from "@/components/charts";

function Kpi({ label, value, sub }: { label: string; value: string; sub?: string }) {
  return (
    <Card className="px-5 py-4">
      <p className="eyebrow">{label}</p>
      <p className="text-[26px] font-semibold tabular mt-1 leading-none" style={{ fontFamily: "var(--font-bricolage)" }}>{value}</p>
      {sub && <p className="text-[12px] text-muted mt-1">{sub}</p>}
    </Card>
  );
}

export default function Dashboard() {
  const { user, has } = useAuth();
  const firstName = user?.fullName?.split(" ")[0] ?? "there";

  const summary = useQuery({ queryKey: ["analytics", "summary"], queryFn: () => fetchSummary() });
  const pipeline = useQuery({
    queryKey: ["analytics", "pipeline"], enabled: has("REPORT_VIEW"),
    queryFn: () => fetchPipelineByStage(),
  });
  // A small, bounded fetch for the "top open deals" list (not the whole table).
  const topDeals = useQuery({
    queryKey: ["dashboard", "topDeals"],
    queryFn: async () => (await listRecords<Record>("deals", { limit: 15, sort: "amount,desc" })).items,
  });

  const s = summary.data;
  const stages = pipeline.data ?? [];
  const top = (topDeals.data ?? [])
    .filter((d: any) => !(d.stageExtra && d.stageExtra.closed))
    .slice(0, 6);

  return (
    <div className="flex flex-col gap-5">
      <div>
        <p className="eyebrow">This quarter</p>
        <h1 className="text-[28px] leading-tight">Good to see you, {firstName}.</h1>
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        <Kpi label="Pipeline value" value={s ? money(s.pipelineValue) : "—"} sub={s ? `${s.openDeals} open deals` : ""} />
        <Kpi label="Expected revenue" value={s ? money(s.expectedRevenue) : "—"} sub="weighted by stage" />
        <Kpi label="Win rate" value={s ? `${s.winRate}%` : "—"} sub={s ? `${s.wonDeals} won` : ""} />
        <Kpi label="Open leads" value={s ? String(s.openLeads) : "—"} sub="awaiting follow-up" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-[1.2fr_1fr] gap-4 items-start">
        {has("REPORT_VIEW") && (
          <Card className="p-0 overflow-hidden">
            <div className="px-5 h-12 flex items-center justify-between border-b border-subtle">
              <p className="font-medium">Pipeline by stage</p>
              <Link href="/reports" className="text-[13px] text-link">All reports</Link>
            </div>
            <div className="p-3">
              {stages.length > 0 ? (
                <EChart height={260} option={barOption({ categories: stages.map((b) => b.label), values: stages.map((b) => Number(b.amount ?? 0)), money: true })} />
              ) : (
                <p className="px-2 py-10 text-center text-[13px] text-muted">No open pipeline yet.</p>
              )}
            </div>
          </Card>
        )}

        <Card className="p-0 overflow-hidden">
          <div className="px-5 h-12 flex items-center justify-between border-b border-subtle">
            <p className="font-medium">Top open deals</p>
            <Link href="/deals" className="text-[13px] text-link">View all deals</Link>
          </div>
          <div>
            {top.map((d: any) => (
              <Link key={d.id} href={`/deals/${d.id}`} className="flex items-center gap-3 px-5 h-12 border-b border-subtle last:border-0 hover:bg-hover">
                <InitialChip name={d.accountName} size={26} square />
                <div className="min-w-0">
                  <p className="text-[14px] font-medium truncate">{d.name}</p>
                  <p className="text-[12px] text-muted">{d.stage} · {d.accountName}</p>
                </div>
                <span className="ml-auto tabular text-[14px] font-medium">{money(d.amount)}</span>
              </Link>
            ))}
            {top.length === 0 && <p className="px-5 py-6 text-[13px] text-muted">No open deals yet.</p>}
          </div>
        </Card>
      </div>
    </div>
  );
}
