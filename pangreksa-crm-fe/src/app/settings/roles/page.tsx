"use client";

import * as React from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Plus, Pencil, Trash2 } from "lucide-react";
import { ApiError, createRecord, deleteRecord, getRecord, listRecords, updateRecord } from "@/lib/api";
import { Badge, Button, Card, Dialog, Input, Label, Select, Textarea } from "@/components/ui";

type RoleRow = { id: number; name: string; description: string | null; parentRoleId: number | null; parentRoleName: string | null; permissions: string[] };

function useRoles() {
  return useQuery({ queryKey: ["roles"], queryFn: async () => (await listRecords<RoleRow>("roles", { limit: 500 })).items });
}
function usePermissions() {
  return useQuery({ queryKey: ["permissions"], queryFn: () => getRecord<string[]>("roles", "permissions") });
}

function groupPermissions(perms: string[]): { group: string; items: string[] }[] {
  const modules = ["LEAD", "ACCOUNT", "CONTACT", "DEAL"];
  const groups: { group: string; items: string[] }[] = [];
  for (const m of modules) {
    const items = perms.filter((p) => p.startsWith(`${m}_`));
    if (items.length) groups.push({ group: m.charAt(0) + m.slice(1).toLowerCase() + "s", items });
  }
  const general = perms.filter((p) => !modules.some((m) => p.startsWith(`${m}_`)));
  if (general.length) groups.unshift({ group: "General", items: general });
  return groups;
}
const labelOf = (p: string) => p.split("_").slice(1).join(" ").toLowerCase() || p.toLowerCase().replace(/_/g, " ");

type FormState = { name: string; description: string; parentRoleId: string; permissions: Set<string> };
const empty = (): FormState => ({ name: "", description: "", parentRoleId: "", permissions: new Set() });

export default function RolesPage() {
  const qc = useQueryClient();
  const roles = useRoles();
  const perms = usePermissions();
  const [editing, setEditing] = React.useState<RoleRow | null>(null);
  const [open, setOpen] = React.useState(false);
  const [form, setForm] = React.useState<FormState>(empty());
  const [confirmDel, setConfirmDel] = React.useState<RoleRow | null>(null);

  const openNew = () => { setEditing(null); setForm(empty()); setOpen(true); };
  const openEdit = (r: RoleRow) => {
    setEditing(r);
    setForm({ name: r.name, description: r.description ?? "", parentRoleId: r.parentRoleId ? String(r.parentRoleId) : "", permissions: new Set(r.permissions) });
    setOpen(true);
  };

  const toggle = (p: string) => setForm((f) => {
    const next = new Set(f.permissions);
    next.has(p) ? next.delete(p) : next.add(p);
    return { ...f, permissions: next };
  });

  const save = useMutation({
    mutationFn: async () => {
      const body = {
        name: form.name, description: form.description || null,
        parentRoleId: form.parentRoleId ? Number(form.parentRoleId) : null,
        permissions: Array.from(form.permissions),
      };
      if (editing) return updateRecord("roles", editing.id, body);
      return createRecord("roles", body);
    },
    onSuccess: () => { toast.success(editing ? "Role updated" : "Role created"); setOpen(false); qc.invalidateQueries({ queryKey: ["roles"] }); },
    onError: (e) => toast.error(e instanceof ApiError ? e.message : "Couldn't save role"),
  });

  const remove = useMutation({
    mutationFn: (id: number) => deleteRecord("roles", id),
    onSuccess: () => { toast.success("Role deleted"); setConfirmDel(null); qc.invalidateQueries({ queryKey: ["roles"] }); },
    onError: (e) => toast.error(e instanceof ApiError ? e.message : "Couldn't delete role"),
  });

  const rows = roles.data ?? [];
  const groups = groupPermissions(perms.data ?? []);

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center">
        <p className="text-[14px] text-muted">{rows.length} role{rows.length === 1 ? "" : "s"}</p>
        <Button variant="primary" className="ml-auto" onClick={openNew}><Plus size={16} /> New role</Button>
      </div>

      <Card className="overflow-hidden">
        <table className="w-full text-[14px]">
          <thead>
            <tr className="text-left border-b border-subtle">
              {["Role", "Reports to", "Permissions", ""].map((h) => <th key={h} className="eyebrow font-semibold px-4 h-10">{h}</th>)}
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.id} className="border-b border-subtle last:border-0 hover:bg-hover">
                <td className="px-4 h-12">
                  <span className="font-medium text-fg">{r.name}</span>
                  {r.description && <span className="block text-[12px] text-muted">{r.description}</span>}
                </td>
                <td className="px-4 h-12 text-secondary">{r.parentRoleName || "—"}</td>
                <td className="px-4 h-12"><Badge tone="accent">{r.permissions.length}</Badge></td>
                <td className="px-4 h-12 text-right whitespace-nowrap">
                  <Button variant="ghost" size="icon" aria-label="Edit" onClick={() => openEdit(r)}><Pencil size={16} /></Button>
                  <Button variant="ghost" size="icon" aria-label="Delete" onClick={() => setConfirmDel(r)}><Trash2 size={16} /></Button>
                </td>
              </tr>
            ))}
            {!roles.isLoading && rows.length === 0 && (
              <tr><td colSpan={4} className="px-4 py-8 text-center text-[13px] text-muted">No roles yet.</td></tr>
            )}
          </tbody>
        </table>
      </Card>

      <Dialog open={open} onClose={() => setOpen(false)} title={editing ? "Edit role" : "New role"} wide
        footer={<><Button variant="ghost" onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="primary" onClick={() => save.mutate()} disabled={save.isPending}>{save.isPending ? "Saving…" : "Save"}</Button></>}>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div>
            <Label>Role name</Label>
            <Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="Sales Rep" />
          </div>
          <div>
            <Label>Reports to</Label>
            <Select value={form.parentRoleId} onChange={(e) => setForm({ ...form, parentRoleId: e.target.value })}>
              <option value="">— Top level —</option>
              {rows.filter((r) => r.id !== editing?.id).map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}
            </Select>
          </div>
          <div className="sm:col-span-2">
            <Label>Description</Label>
            <Textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} className="min-h-16" />
          </div>
        </div>
        <div className="mt-4">
          <p className="eyebrow mb-2">Permissions</p>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-4">
            {groups.map((g) => (
              <div key={g.group}>
                <p className="text-[13px] font-medium mb-1.5">{g.group}</p>
                <div className="flex flex-col gap-1.5">
                  {g.items.map((p) => (
                    <label key={p} className="flex items-center gap-2 text-[13px] text-secondary cursor-pointer">
                      <input type="checkbox" checked={form.permissions.has(p)} onChange={() => toggle(p)} className="h-4 w-4 accent-[var(--color-accent)]" />
                      <span className="capitalize">{labelOf(p)}</span>
                      <span className="text-muted text-[11px] tabular ml-auto">{p}</span>
                    </label>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
      </Dialog>

      <Dialog open={!!confirmDel} onClose={() => setConfirmDel(null)} title="Delete role?"
        footer={<><Button variant="ghost" onClick={() => setConfirmDel(null)}>Cancel</Button>
          <Button variant="danger" onClick={() => confirmDel && remove.mutate(confirmDel.id)} disabled={remove.isPending}>Delete</Button></>}>
        <p className="text-[14px] text-secondary">This can&apos;t be undone. Users assigned to {confirmDel?.name} will need a new role.</p>
      </Dialog>
    </div>
  );
}
