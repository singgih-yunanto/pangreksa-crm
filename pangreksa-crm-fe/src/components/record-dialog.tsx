"use client";

import * as React from "react";
import { useForm, type UseFormRegister, type UseFormWatch } from "react-hook-form";
import { toast } from "sonner";
import { ApiError } from "@/lib/api";
import { useAllRecords, useCreate, useLookups, useUpdate } from "@/lib/hooks";
import { RELATED_TO_MODULES, WHO_MODULES, MODULES, type FormField, type ModuleConfig } from "@/lib/modules";
import { Button, Dialog, Input, Label, Select, Textarea } from "@/components/ui";

function recordLabel(r: any): string {
  return r.name ?? r.fullName ?? r.title ?? r.subject ?? r.lastName ?? `#${r.id}`;
}

function LookupField({ field, register, defaultValue }: { field: FormField; register: UseFormRegister<any>; defaultValue?: any }) {
  const lookups = useLookups(field.lookupCategory);
  const records = useAllRecords(field.lookupEndpoint ?? "", !!field.lookupEndpoint);
  const options: { value: string | number; label: string }[] = field.lookupCategory
    ? (lookups.data ?? []).map((l) => ({ value: l.id, label: l.label }))
    : (records.data ?? []).map((r: any) => ({ value: r.id, label: recordLabel(r) }));
  return (
    <Select id={field.name} defaultValue={defaultValue ?? ""} {...register(field.name)}>
      <option value="">— None —</option>
      {options.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
    </Select>
  );
}

/** Two-part polymorphic picker: a module select + a record select fed by useAllRecords for that module. */
function PolymorphicField({ typeKey, idKey, modules, register, watch }: {
  typeKey: string; idKey: string; modules: string[]; register: UseFormRegister<any>; watch: UseFormWatch<any>;
}) {
  const type = watch(typeKey) as string | undefined;
  const records = useAllRecords(type ?? "", !!type);
  return (
    <div className="grid grid-cols-2 gap-2">
      <Select {...register(typeKey)}>
        <option value="">— None —</option>
        {modules.map((m) => <option key={m} value={m}>{MODULES[m].singular}</option>)}
      </Select>
      <Select {...register(idKey)} disabled={!type}>
        <option value="">— Select —</option>
        {(records.data ?? []).map((r: any) => <option key={r.id} value={r.id}>{recordLabel(r)}</option>)}
      </Select>
    </div>
  );
}

export function RecordDialog({ module, record, open, onClose, defaults }: {
  module: ModuleConfig; record?: any | null; open: boolean; onClose: () => void; defaults?: Record<string, any>;
}) {
  const editing = !!record?.id;
  const { register, handleSubmit, setError, reset, watch, formState: { errors } } = useForm();
  const create = useCreate(module.endpoint);
  const update = useUpdate(module.endpoint);

  React.useEffect(() => {
    const init: any = { ...(record ?? {}), ...(defaults ?? {}) };
    // datetime-local inputs want "YYYY-MM-DDTHH:mm" — trim any seconds/zone off ISO values.
    for (const f of module.form) {
      if (f.type === "datetime" && init[f.name]) init[f.name] = String(init[f.name]).slice(0, 16);
    }
    reset(init);
  }, [record, open, reset, module, defaults]);

  const onSubmit = handleSubmit(async (values) => {
    const body: any = {};
    for (const f of module.form) {
      if (f.type === "relatedTo") {
        if (values.whatType && values.whatId) { body.whatType = values.whatType; body.whatId = Number(values.whatId); }
        continue;
      }
      if (f.type === "who") {
        if (values.whoType && values.whoId) { body.whoType = values.whoType; body.whoId = Number(values.whoId); }
        continue;
      }
      let v: any = values[f.name];
      if (v === "" || v == null) continue;
      if (f.type === "number" || f.type === "currency" || f.type === "lookup") v = Number(v);
      // datetime stays as the raw "YYYY-MM-DDTHH:mm" local string (parsed as LocalDateTime server-side).
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
          const wide = f.type === "textarea" || f.type === "relatedTo" || f.type === "who";
          return (
            <div key={f.name} className={wide ? "sm:col-span-2" : ""}>
              <Label htmlFor={f.name}>{f.label}{f.required && <span className="text-accent"> *</span>}</Label>
              {f.type === "textarea" ? (
                <Textarea id={f.name} {...register(f.name)} />
              ) : f.type === "relatedTo" ? (
                <PolymorphicField typeKey="whatType" idKey="whatId" modules={RELATED_TO_MODULES} register={register} watch={watch} />
              ) : f.type === "who" ? (
                <PolymorphicField typeKey="whoType" idKey="whoId" modules={WHO_MODULES} register={register} watch={watch} />
              ) : f.type === "lookup" ? (
                <LookupField field={f} register={register} defaultValue={record?.[f.name]} />
              ) : (
                <Input id={f.name}
                  type={f.type === "date" ? "date" : f.type === "datetime" ? "datetime-local"
                    : f.type === "number" || f.type === "currency" ? "number" : f.type === "email" ? "email" : "text"}
                  {...register(f.name)} />
              )}
              {err && <p role="alert" className="mt-1 text-[12px] text-danger">{err}</p>}
            </div>
          );
        })}
      </form>
    </Dialog>
  );
}
