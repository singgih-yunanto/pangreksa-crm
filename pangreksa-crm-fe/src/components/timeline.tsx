"use client";

import * as React from "react";
import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { History, CheckSquare, CalendarDays, Phone, StickyNote, Paperclip, Download, Trash2, Send } from "lucide-react";
import {
  addNote, deleteAttachment, downloadAttachment, fetchTimeline, listAttachments, uploadAttachment,
  type TimelineItem,
} from "@/lib/api";
import { Button, Textarea } from "@/components/ui";

const KIND_ICON: Record<string, React.ComponentType<{ size?: number; className?: string }>> = {
  audit: History, task: CheckSquare, meeting: CalendarDays, call: Phone, note: StickyNote,
};

const when = (iso: string) => String(iso).slice(0, 16).replace("T", " ");
const kb = (n: number) => (n < 1024 ? `${n} B` : n < 1024 * 1024 ? `${(n / 1024).toFixed(0)} KB` : `${(n / 1024 / 1024).toFixed(1)} MB`);

export function Timeline({ moduleKey, recordId }: { moduleKey: string; recordId: number }) {
  const qc = useQueryClient();
  const [note, setNote] = React.useState("");
  const fileRef = React.useRef<HTMLInputElement>(null);

  const timeline = useQuery({
    queryKey: ["timeline", moduleKey, recordId],
    queryFn: () => fetchTimeline(moduleKey, recordId),
  });
  const attachments = useQuery({
    queryKey: ["attachments", moduleKey, recordId],
    queryFn: () => listAttachments(moduleKey, recordId),
  });

  const invalidateTimeline = () => qc.invalidateQueries({ queryKey: ["timeline", moduleKey, recordId] });

  const saveNote = useMutation({
    mutationFn: () => addNote(moduleKey, recordId, note.trim()),
    onSuccess: () => { setNote(""); invalidateTimeline(); toast.success("Note added"); },
    onError: () => toast.error("Couldn't add note — try again."),
  });
  const upload = useMutation({
    mutationFn: (file: File) => uploadAttachment(moduleKey, recordId, file),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["attachments", moduleKey, recordId] }); toast.success("File uploaded"); },
    onError: () => toast.error("Upload failed — max 10 MB."),
  });
  const removeAttachment = useMutation({
    mutationFn: (id: number) => deleteAttachment(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["attachments", moduleKey, recordId] }),
    onError: () => toast.error("Couldn't delete — try again."),
  });

  const items = timeline.data ?? [];
  const files = attachments.data ?? [];

  return (
    <div className="flex flex-col gap-5">
      {/* Composer */}
      <div className="flex flex-col gap-2">
        <Textarea placeholder="Add a note…" value={note} onChange={(e) => setNote(e.target.value)} className="min-h-16" />
        <div className="flex items-center gap-2">
          <input ref={fileRef} type="file" className="hidden"
            onChange={(e) => { const f = e.target.files?.[0]; if (f) upload.mutate(f); e.target.value = ""; }} />
          <Button variant="secondary" size="sm" onClick={() => fileRef.current?.click()} disabled={upload.isPending}>
            <Paperclip size={14} /> {upload.isPending ? "Uploading…" : "Attach"}
          </Button>
          <Button variant="primary" size="sm" className="ml-auto" onClick={() => saveNote.mutate()} disabled={!note.trim() || saveNote.isPending}>
            <Send size={14} /> Add note
          </Button>
        </div>
      </div>

      {/* Attachments */}
      {files.length > 0 && (
        <div className="flex flex-col gap-1.5">
          <p className="eyebrow">Attachments <span className="text-muted">({files.length})</span></p>
          {files.map((f) => (
            <div key={f.id} className="flex items-center gap-2 text-[13px] rounded-[8px] border border-subtle px-2.5 py-1.5">
              <Paperclip size={14} className="text-muted shrink-0" />
              <button onClick={() => downloadAttachment(f.id, f.filename)} className="font-medium text-fg hover:text-accent cursor-pointer truncate">{f.filename}</button>
              <span className="text-muted tabular shrink-0">{kb(f.sizeBytes)}</span>
              <Button variant="ghost" size="icon" className="ml-auto h-7 w-7" aria-label="Download" onClick={() => downloadAttachment(f.id, f.filename)}><Download size={14} /></Button>
              <Button variant="ghost" size="icon" className="h-7 w-7" aria-label="Delete attachment" onClick={() => removeAttachment.mutate(f.id)}><Trash2 size={14} /></Button>
            </div>
          ))}
        </div>
      )}

      {/* Feed */}
      <div className="flex flex-col">
        {items.length === 0 && !timeline.isLoading && <p className="text-[13px] text-muted">No activity yet.</p>}
        {items.map((it: TimelineItem, i: number) => {
          const Icon = KIND_ICON[it.kind] ?? History;
          const heading = it.kind === "note" ? "Note" : it.title;
          return (
            <div key={i} className="flex gap-3 pb-4 last:pb-0">
              <div className="flex flex-col items-center">
                <span className="w-7 h-7 rounded-full bg-accent-soft text-accent-soft-fg inline-flex items-center justify-center shrink-0"><Icon size={14} /></span>
                {i < items.length - 1 && <span className="w-px flex-1 bg-subtle mt-1" />}
              </div>
              <div className="flex-1 min-w-0 -mt-0.5">
                <div className="flex items-baseline gap-2">
                  {it.refModule && it.refId ? (
                    <Link href={`/${it.refModule}/${it.refId}`} className="text-[13px] font-medium text-fg hover:text-accent truncate">{heading}</Link>
                  ) : (
                    <p className="text-[13px] font-medium text-fg truncate">{heading}</p>
                  )}
                  <span className="text-[11px] text-muted tabular ml-auto shrink-0">{when(it.at)}</span>
                </div>
                {(it.subtitle || it.actor) && it.kind !== "note" && (
                  <p className="text-[12px] text-muted truncate">
                    {[it.kind !== "audit" ? it.subtitle : null, it.actor].filter(Boolean).join(" · ")}
                  </p>
                )}
                {it.kind === "note" && (
                  <>
                    <p className="text-[13px] text-secondary mt-0.5 whitespace-pre-wrap">{it.title}</p>
                    {it.actor && <p className="text-[12px] text-muted mt-0.5">{it.actor}</p>}
                  </>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
