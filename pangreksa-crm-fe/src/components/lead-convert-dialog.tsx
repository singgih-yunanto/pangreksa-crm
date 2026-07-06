"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { convertLead, type LeadConvertBody } from "@/lib/api";
import { useAllRecords, useLookups } from "@/lib/hooks";
import { Button, Dialog, Input, Label, Select } from "@/components/ui";

export function LeadConvertDialog({ lead, open, onClose }: { lead: any; open: boolean; onClose: () => void }) {
  const router = useRouter();
  const qc = useQueryClient();
  const stages = useLookups(open ? "deal_stage" : undefined);
  const accounts = useAllRecords("accounts", open);
  const contacts = useAllRecords("contacts", open);

  const [accountId, setAccountId] = React.useState("");
  const [contactId, setContactId] = React.useState("");
  const [createDeal, setCreateDeal] = React.useState(true);
  const [dealName, setDealName] = React.useState("");
  const [stageId, setStageId] = React.useState("");
  const [amount, setAmount] = React.useState("");
  const [closingDate, setClosingDate] = React.useState("");
  const [busy, setBusy] = React.useState(false);

  React.useEffect(() => {
    if (open) {
      const base = lead.company || lead.fullName || "New";
      setDealName(`${base} — new deal`);
      setAccountId(""); setContactId(""); setCreateDeal(true); setStageId(""); setAmount(""); setClosingDate("");
    }
  }, [open, lead]);

  const submit = async () => {
    setBusy(true);
    try {
      const body: LeadConvertBody = {
        accountId: accountId ? Number(accountId) : undefined,
        contactId: contactId ? Number(contactId) : undefined,
        createDeal,
        dealName: createDeal ? dealName : undefined,
        dealStageId: createDeal && stageId ? Number(stageId) : undefined,
        dealAmount: createDeal && amount ? Number(amount) : undefined,
        dealClosingDate: createDeal && closingDate ? closingDate : undefined,
      };
      const res = await convertLead(lead.id, body);
      toast.success("Lead converted");
      qc.invalidateQueries({ queryKey: ["record", "leads", String(lead.id)] });
      onClose();
      router.push(`/accounts/${res.accountId}`);
    } catch {
      toast.error("Conversion failed — try again.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} title="Convert lead"
      footer={
        <>
          <Button variant="ghost" onClick={onClose} disabled={busy}>Cancel</Button>
          <Button variant="primary" onClick={submit} disabled={busy}>{busy ? "Converting…" : "Convert lead"}</Button>
        </>
      }>
      <div className="flex flex-col gap-3">
        <div>
          <Label>Account</Label>
          <Select value={accountId} onChange={(e) => setAccountId(e.target.value)}>
            <option value="">Create new from lead</option>
            {(accounts.data ?? []).map((a: any) => <option key={a.id} value={a.id}>{a.name}</option>)}
          </Select>
        </div>
        <div>
          <Label>Contact</Label>
          <Select value={contactId} onChange={(e) => setContactId(e.target.value)}>
            <option value="">Create new from lead</option>
            {(contacts.data ?? []).map((c: any) => <option key={c.id} value={c.id}>{c.fullName ?? c.lastName}</option>)}
          </Select>
        </div>

        <label className="flex items-center gap-2 text-[14px] cursor-pointer select-none mt-1">
          <input type="checkbox" checked={createDeal} onChange={(e) => setCreateDeal(e.target.checked)} />
          Create a deal
        </label>

        {createDeal && (
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-4 gap-y-3 rounded-[10px] border border-subtle p-3">
            <div className="sm:col-span-2">
              <Label>Deal name</Label>
              <Input value={dealName} onChange={(e) => setDealName(e.target.value)} />
            </div>
            <div>
              <Label>Stage</Label>
              <Select value={stageId} onChange={(e) => setStageId(e.target.value)}>
                <option value="">— Default —</option>
                {(stages.data ?? []).map((l) => <option key={l.id} value={l.id}>{l.label}</option>)}
              </Select>
            </div>
            <div>
              <Label>Amount</Label>
              <Input type="number" value={amount} onChange={(e) => setAmount(e.target.value)} />
            </div>
            <div>
              <Label>Closing date</Label>
              <Input type="date" value={closingDate} onChange={(e) => setClosingDate(e.target.value)} />
            </div>
          </div>
        )}
      </div>
    </Dialog>
  );
}
