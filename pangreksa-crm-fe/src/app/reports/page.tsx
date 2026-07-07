"use client";

import * as React from "react";
import { useQuery } from "@tanstack/react-query";
import { Download } from "lucide-react";
import {
  fetchActivityStats, fetchLeadsByStatus, fetchPipelineByStage, fetchSalesByOwner, listRecords,
  type AnalyticsFilter, type Record,
} from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { money } from "@/lib/format";
import { downloadRowsCsv } from "@/lib/export";
import { Button, Card, Select } from "@/components/ui";
import { EChart, barOption, donutOption, stackedBarOption } from "@/components/charts";

function isoDate(d: Date) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}
function periodRange(p: string): { from?: string; to?: string } {
  const now = new Date();
  const y = now.getFullYear();
  if (p === "quarter") {
    const q = Math.floor(now.getMonth() / 3);
    return { from: isoDate(new Date(y, q * 3, 1)), to: isoDate(new Date(y, q * 3 + 3, 0)) };
  }
  if (p === "year") return { from: `${y}-01-01`, to: `${y}-12-31` };
  return {};
}

function ReportCard({ title, rows, filename, children }: {
  title: string; rows: Record[] | { [k: string]: unknown }[]; filename: string; children: React.ReactNode;
}) {
  return (
    <Card className="p-0 overflow-hidden">
      <div className="px-5 h-12 flex items-center justify-between border-b border-subtle">
        <p className="font-medium">{title}</p>
        <Button variant="ghost" size="sm" onClick={() => downloadRowsCsv(filename, rows as { [k: string]: unknown }[])} disabled={rows.length === 0}>
          <Download size={14} /> Export
        </Button>
      </div>
      <div className="p-3">{children}</div>
    </Card>
  );
}

export default function ReportsPage() {
  const { has } = useAuth();
  const isAdmin = has("ADMIN_USERS");
  const [period, setPeriod] = React.useState("year");
  const [ownerId, setOwnerId] = React.useState<string>("");

  const filter: AnalyticsFilter = { ...periodRange(period), ownerId: ownerId ? Number(ownerId) : "" };
  const key = [filter.from ?? "", filter.to ?? "", filter.ownerId ?? ""];

  const users = useQuery({
    queryKey: ["users", "for-reports"], enabled: isAdmin,
    queryFn: async () => (await listRecords<Record>("users", { limit: 500 })).items,
  });
  const pipeline = useQuery({ queryKey: ["r", "pipeline", ...key], queryFn: () => fetchPipelineByStage(filter) });
  const sales = useQuery({ queryKey: ["r", "sales", ...key], queryFn: () => fetchSalesByOwner(filter) });
  const leads = useQuery({ queryKey: ["r", "leads", ...key], queryFn: () => fetchLeadsByStatus(filter) });
  const acts = useQuery({ queryKey: ["r", "acts", ...key], queryFn: () => fetchActivityStats(filter) });

  const pipelineData = pipeline.data ?? [];
  const salesData = sales.data ?? [];
  const leadData = leads.data;
  const actData = acts.data;

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center gap-3 flex-wrap">
        <div>
          <p className="eyebrow">Analytics</p>
          <h1 className="text-[26px] leading-tight">Reports</h1>
        </div>
        <div className="ml-auto flex items-center gap-2">
          {isAdmin && (
            <Select value={ownerId} onChange={(e) => setOwnerId(e.target.value)} className="w-48">
              <option value="">All owners (my scope)</option>
              {(users.data ?? []).map((u: any) => <option key={u.id} value={u.id}>{u.fullName}</option>)}
            </Select>
          )}
          <Select value={period} onChange={(e) => setPeriod(e.target.value)} className="w-40">
            <option value="quarter">This quarter</option>
            <option value="year">This year</option>
            <option value="all">All time</option>
          </Select>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <ReportCard title="Pipeline by stage" filename="pipeline-by-stage"
          rows={pipelineData.map((b) => ({ stage: b.label, count: b.count, amount: b.amount }))}>
          {pipelineData.length ? (
            <EChart height={300} option={barOption({ categories: pipelineData.map((b) => b.label), values: pipelineData.map((b) => Number(b.amount ?? 0)), money: true })} />
          ) : <Empty />}
        </ReportCard>

        <ReportCard title="Sales by rep" filename="sales-by-rep"
          rows={salesData.map((o) => ({ owner: o.owner, deals: o.count, won: o.won, open: o.open, total: o.total }))}>
          {salesData.length ? (
            <EChart height={300} option={stackedBarOption({
              categories: salesData.map((o) => o.owner),
              series: [
                { name: "Won", values: salesData.map((o) => Number(o.won)) },
                { name: "Open", values: salesData.map((o) => Number(o.open)) },
              ],
              money: true,
            })} />
          ) : <Empty />}
        </ReportCard>

        <ReportCard title="Leads by status" filename="leads-by-status"
          rows={(leadData?.byStatus ?? []).map((b) => ({ status: b.label, count: b.count }))}>
          {leadData && leadData.byStatus.length ? (
            <>
              <EChart height={270} option={donutOption({ items: leadData.byStatus.map((b) => ({ name: b.label, value: b.count })) })} />
              <p className="text-center text-[13px] text-muted -mt-2">{leadData.converted} converted</p>
            </>
          ) : <Empty />}
        </ReportCard>

        <ReportCard title="Activities overview" filename="activities"
          rows={actData ? [{ tasks: actData.tasks, meetings: actData.meetings, calls: actData.calls }] : []}>
          {actData ? (
            <EChart height={300} option={barOption({
              categories: ["Tasks", "Meetings", "Calls"],
              values: [actData.tasks, actData.meetings, actData.calls],
            })} />
          ) : <Empty />}
        </ReportCard>
      </div>
    </div>
  );
}

function Empty() {
  return <p className="px-2 py-12 text-center text-[13px] text-muted">No data for this period.</p>;
}
