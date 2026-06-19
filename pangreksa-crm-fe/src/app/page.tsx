"use client";

import Link from "next/link";
import { useAllRecords } from "@/lib/hooks";
import { money } from "@/lib/format";
import { Card, InitialChip } from "@/components/ui";

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
  const deals = useAllRecords("deals").data ?? [];
  const leads = useAllRecords("leads").data ?? [];
  const open = deals.filter((d: any) => !(d.stageExtra && d.stageExtra.closed));
  const pipeline = open.reduce((n, d: any) => n + (Number(d.amount) || 0), 0);
  const expected = open.reduce((n, d: any) => n + (Number(d.expectedRevenue) || 0), 0);
  const won = deals.filter((d: any) => d.stageExtra && d.stageExtra.won).length;
  const winRate = deals.length ? Math.round((won / deals.length) * 100) : 0;
  const top = [...open].sort((a: any, b: any) => (b.amount || 0) - (a.amount || 0)).slice(0, 6);

  return (
    <div className="flex flex-col gap-5">
      <div>
        <p className="eyebrow">This quarter</p>
        <h1 className="text-[28px] leading-tight">Good to see you, Singgih.</h1>
      </div>
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        <Kpi label="Pipeline value" value={money(pipeline)} sub={`${open.length} open deals`} />
        <Kpi label="Expected revenue" value={money(expected)} sub="weighted by stage" />
        <Kpi label="Win rate" value={`${winRate}%`} sub={`${won} won`} />
        <Kpi label="Open leads" value={String(leads.length)} sub="awaiting follow-up" />
      </div>
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
  );
}
