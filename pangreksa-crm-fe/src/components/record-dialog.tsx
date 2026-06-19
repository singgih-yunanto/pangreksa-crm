"use client";

import * as React from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { ApiError } from "@/lib/api";
import { useAllRecords, useCreate, useLookups, useUpdate } from "@/lib/hooks";
import type { FormField, ModuleConfig } from "@/lib/modules";
import { Button, Dialog, Input, Label, Select, Textarea } from "@/components/ui";

function LookupField({ field, register, defaultValue }: { field: FormField; register: any; defaultValue?: any }) {
  const lookups = useLookups(field.lookupCategory);
  const records = useAllRecords(field.lookupEndpoint ?? "", !!field.lookupEndpoint);
  const options: { value: string | number; label: string }[] = field.lookupCategory
    ? (lookups.data ?? []).map((l) => ({ value: l.id, label: l.label }))
    : (records.data ?? []).map((r: any) => ({ value: r.id, label: r.name ?? r.fullName ?? r.lastName }));
  return (
    <Select id={field.name} defaultValue={defaultValue ?? ""} {...register(field.name)}>
      <option value="">— None —</option>
      {options.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
    </Select>
  );
}

export function RecordDialog({ module, record, open, onClose }: {
  module: ModuleConfig; record?: any | null; open: boolean; onClose: () => void;
}) {
  const editing = !!record?.id;
  const { register, handleSubmit, setError, reset, formState: { errors } } = useForm();
  const create = useCreate(module.endpoint);
  const update = useUpdate(module.endpoint);

  React.useEffect(() => { reset(record ?? {}); }, [record, open, reset]);

  const onSubmit = handleSubmit(async (values) => {
    const body: any = {};
    for (const f of module.form) {
      let v: any = values[f.name];
      if (v === "" || v == null) continue;
      if (f.type === "number" || f.type === "currency" || f.type === "lookup") v = Number(v);
      body[f.name] = v;
    }
    try {
      if (editing) await update.mutateAsync({ id: record.id, body });
      else await create.mutateAsync(body);
      toast.success(`${module.singular} ${editing ? "updated" : "created"}`);
      onClose();
    } catch (e) {
      if (e instanceof ApiError) {
        if (e.errors.length) e.errors.forEach((fe) => fe.field && setError(fe.field, { message: fe.message }));
        else toast.error(e.message);
        if (e.errors.length) toast.error(e.message);
      } else {
        toast.error("Couldn't save — check your connection and try again.");
      }
    }
  });

  const busy = create.isPending || update.isPending;

  return (
    <Dialog open={open} onClose={onClose} wide
      title={editing ? `Edit ${module.singular.toLowerCase()}` : `New ${module.singular.toLowerCase()}`}
      footer={
        <>
          <Button variant="ghost" onClick={onClose} disabled={busy}>Cancel</Button>
          <Button variant="primary" onClick={onSubmit} disabled={busy}>
            {busy ? "Saving…" : editing ? "Save changes" : `Create ${module.singular.toLowerCase()}`}
          </Button>
        </>
      }>
      <form onSubmit={onSubmit} className="grid grid-cols-1 sm:grid-cols-2 gap-x-4 gap-y-3">
        {module.form.map((f) => {
          const err = errors[f.name]?.message as string | undefined;
          return (
            <div key={f.name} className={f.type === "textarea" ? "sm:col-span-2" : ""}>
              <Label htmlFor={f.name}>{f.label}{f.required && <span className="text-accent"> *</span>}</Label>
              {f.type === "textarea" ? (
                <Textarea id={f.name} {...register(f.name)} />
              ) : f.type === "lookup" ? (
                <LookupField field={f} register={register} defaultValue={record?.[f.name]} />
              ) : (
                <Input id={f.name} type={f.type === "date" ? "date" : f.type === "number" || f.type === "currency" ? "number" : f.type === "email" ? "email" : "text"} {...register(f.name)} />
              )}
              {err && <p role="alert" className="mt-1 text-[12px] text-danger">{err}</p>}
            </div>
          );
        })}
      </form>
    </Dialog>
  );
}
