"use client";

import * as React from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Plus, Pencil, Trash2 } from "lucide-react";
import { ApiError, createRecord, deleteRecord, listRecords, updateRecord } from "@/lib/api";
import { Badge, Button, Card, Dialog, InitialChip, Input, Label, Select } from "@/components/ui";

type UserRow = { id: number; username: string; fullName: string; email: string | null; active: boolean; roleId: number | null; roleName: string | null };
type RoleRow = { id: number; name: string };

function useUsers() {
  return useQuery({ queryKey: ["users"], queryFn: async () => (await listRecords<UserRow>("users", { limit: 500 })).items });
}
function useRoles() {
  return useQuery({ queryKey: ["roles"], queryFn: async () => (await listRecords<RoleRow>("roles", { limit: 500 })).items });
}

type FormState = { username: string; fullName: string; email: string; password: string; roleId: string; active: boolean };
const empty: FormState = { username: "", fullName: "", email: "", password: "", roleId: "", active: true };

export default function UsersPage() {
  const qc = useQueryClient();
  const users = useUsers();
  const roles = useRoles();
  const [editing, setEditing] = React.useState<UserRow | null>(null);
  const [open, setOpen] = React.useState(false);
  const [form, setForm] = React.useState<FormState>(empty);
  const [confirmDel, setConfirmDel] = React.useState<UserRow | null>(null);

  const openNew = () => { setEditing(null); setForm(empty); setOpen(true); };
  const openEdit = (u: UserRow) => {
    setEditing(u);
    setForm({ username: u.username, fullName: u.fullName, email: u.email ?? "", password: "", roleId: u.roleId ? String(u.roleId) : "", active: u.active });
    setOpen(true);
  };

  const save = useMutation({
    mutationFn: async () => {
      const body: Record<string, unknown> = {
        fullName: form.fullName, email: form.email || null, active: form.active,
        roleId: form.roleId ? Number(form.roleId) : null,
      };
      if (form.password) body.password = form.password;
      if (editing) return updateRecord("users", editing.id, body);
      return createRecord("users", { ...body, username: form.username, password: form.password });
    },
    onSuccess: () => { toast.success(editing ? "User updated" : "User created"); setOpen(false); qc.invalidateQueries({ queryKey: ["users"] }); },
    onError: (e) => toast.error(e instanceof ApiError ? e.message : "Couldn't save user"),
  });

  const remove = useMutation({
    mutationFn: (id: number) => deleteRecord("users", id),
    onSuccess: () => { toast.success("User deleted"); setConfirmDel(null); qc.invalidateQueries({ queryKey: ["users"] }); },
    onError: () => toast.error("Couldn't delete user"),
  });

  const rows = users.data ?? [];

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center">
        <p className="text-[14px] text-muted">{rows.length} user{rows.length === 1 ? "" : "s"}</p>
        <Button variant="primary" className="ml-auto" onClick={openNew}><Plus size={16} /> New user</Button>
      </div>

      <Card className="overflow-hidden">
        <table className="w-full text-[14px]">
          <thead>
            <tr className="text-left border-b border-subtle">
              {["User", "Email", "Role", "Status", ""].map((h) => <th key={h} className="eyebrow font-semibold px-4 h-10">{h}</th>)}
            </tr>
          </thead>
          <tbody>
            {rows.map((u) => (
              <tr key={u.id} className="border-b border-subtle last:border-0 hover:bg-hover">
                <td className="px-4 h-12">
                  <span className="inline-flex items-center gap-2">
                    <InitialChip name={u.fullName} size={24} />
                    <span><span className="font-medium text-fg">{u.fullName}</span> <span className="text-muted">@{u.username}</span></span>
                  </span>
                </td>
                <td className="px-4 h-12 text-secondary">{u.email || "—"}</td>
                <td className="px-4 h-12 text-secondary">{u.roleName || "—"}</td>
                <td className="px-4 h-12"><Badge tone={u.active ? "success" : "neutral"}>{u.active ? "Active" : "Inactive"}</Badge></td>
                <td className="px-4 h-12 text-right whitespace-nowrap">
                  <Button variant="ghost" size="icon" aria-label="Edit" onClick={() => openEdit(u)}><Pencil size={16} /></Button>
                  <Button variant="ghost" size="icon" aria-label="Delete" onClick={() => setConfirmDel(u)}><Trash2 size={16} /></Button>
                </td>
              </tr>
            ))}
            {!users.isLoading && rows.length === 0 && (
              <tr><td colSpan={5} className="px-4 py-8 text-center text-[13px] text-muted">No users yet.</td></tr>
            )}
          </tbody>
        </table>
      </Card>

      <Dialog open={open} onClose={() => setOpen(false)} title={editing ? "Edit user" : "New user"}
        footer={<><Button variant="ghost" onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="primary" onClick={() => save.mutate()} disabled={save.isPending}>{save.isPending ? "Saving…" : "Save"}</Button></>}>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div className="sm:col-span-1">
            <Label>Username</Label>
            <Input value={form.username} disabled={!!editing} onChange={(e) => setForm({ ...form, username: e.target.value })} placeholder="jdoe" />
          </div>
          <div className="sm:col-span-1">
            <Label>Full name</Label>
            <Input value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} placeholder="Jane Doe" />
          </div>
          <div className="sm:col-span-2">
            <Label>Email</Label>
            <Input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} placeholder="jane@company.com" />
          </div>
          <div className="sm:col-span-1">
            <Label>Role</Label>
            <Select value={form.roleId} onChange={(e) => setForm({ ...form, roleId: e.target.value })}>
              <option value="">— No role —</option>
              {(roles.data ?? []).map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}
            </Select>
          </div>
          <div className="sm:col-span-1">
            <Label>{editing ? "New password" : "Password"}</Label>
            <Input type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })}
              placeholder={editing ? "Leave blank to keep" : "Required"} />
          </div>
          <div className="sm:col-span-2 flex items-center gap-2 pt-1">
            <input id="active" type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} className="h-4 w-4 accent-[var(--color-accent)]" />
            <Label className="mb-0" htmlFor="active">Active</Label>
          </div>
        </div>
      </Dialog>

      <Dialog open={!!confirmDel} onClose={() => setConfirmDel(null)} title="Delete user?"
        footer={<><Button variant="ghost" onClick={() => setConfirmDel(null)}>Cancel</Button>
          <Button variant="danger" onClick={() => confirmDel && remove.mutate(confirmDel.id)} disabled={remove.isPending}>Delete</Button></>}>
        <p className="text-[14px] text-secondary">This can&apos;t be undone. {confirmDel?.fullName} will lose access.</p>
      </Dialog>
    </div>
  );
}
