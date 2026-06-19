"use client";

import * as React from "react";
import { createPortal } from "react-dom";
import { Check, X } from "lucide-react";
import { cn } from "@/lib/cn";
import { avatarColor, initials } from "@/lib/format";

/* ----------------------------- Button ----------------------------- */
type BtnVariant = "primary" | "secondary" | "ghost" | "danger";
type BtnSize = "sm" | "md" | "icon";
const btnBase =
  "inline-flex items-center justify-center gap-1.5 rounded-[8px] font-medium transition-colors cursor-pointer disabled:opacity-55 disabled:cursor-not-allowed select-none";
const btnVariants: Record<BtnVariant, string> = {
  primary: "bg-accent text-on-accent hover:bg-accent-hover shadow-[var(--shadow-sm)]",
  secondary: "bg-card text-fg border border-line hover:bg-hover",
  ghost: "text-secondary hover:bg-hover hover:text-fg",
  danger: "bg-danger text-on-accent hover:opacity-90",
};
const btnSizes: Record<BtnSize, string> = {
  sm: "h-8 px-2.5 text-[13px]",
  md: "h-9 px-3.5 text-[14px]",
  icon: "h-9 w-9",
};
export function Button({
  variant = "secondary", size = "md", className, ...props
}: React.ButtonHTMLAttributes<HTMLButtonElement> & { variant?: BtnVariant; size?: BtnSize }) {
  return <button className={cn(btnBase, btnVariants[variant], btnSizes[size], className)} {...props} />;
}

/* ----------------------------- Card ----------------------------- */
export function Card({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("bg-card border border-subtle rounded-[12px] shadow-[var(--shadow-sm)]", className)} {...props} />;
}

/* ----------------------------- Badge ----------------------------- */
export function Badge({ className, tone = "neutral", ...props }: React.HTMLAttributes<HTMLSpanElement> & { tone?: "neutral" | "accent" | "success" | "warning" | "info" }) {
  const tones: Record<string, string> = {
    neutral: "bg-sunken text-secondary border-subtle",
    accent: "bg-accent-soft text-accent-soft-fg border-transparent",
    success: "bg-success-soft text-success border-transparent",
    warning: "bg-warning-soft text-warning border-transparent",
    info: "bg-info-soft text-info border-transparent",
  };
  return <span className={cn("inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-[12px] font-medium", tones[tone], className)} {...props} />;
}

/* ----------------------------- InitialChip (avatar) ----------------------------- */
export function InitialChip({ name, size = 28, square = false }: { name?: string | null; size?: number; square?: boolean }) {
  return (
    <span
      className={cn("inline-flex items-center justify-center font-semibold text-white shrink-0", square ? "rounded-[8px]" : "rounded-full")}
      style={{ width: size, height: size, background: avatarColor(name), fontSize: size * 0.4 }}
    >
      {initials(name)}
    </span>
  );
}

/* ----------------------------- Inputs ----------------------------- */
const fieldBase =
  "w-full h-9 rounded-[8px] border border-line bg-card px-3 text-[14px] text-fg placeholder:text-muted focus:border-accent";
export const Input = React.forwardRef<HTMLInputElement, React.InputHTMLAttributes<HTMLInputElement>>(
  function Input({ className, ...props }, ref) {
    return <input ref={ref} className={cn(fieldBase, className)} {...props} />;
  },
);
export const Textarea = React.forwardRef<HTMLTextAreaElement, React.TextareaHTMLAttributes<HTMLTextAreaElement>>(
  function Textarea({ className, ...props }, ref) {
    return <textarea ref={ref} className={cn(fieldBase, "h-auto min-h-20 py-2 resize-y", className)} {...props} />;
  },
);
export function Select({ className, children, ...props }: React.SelectHTMLAttributes<HTMLSelectElement>) {
  return <select className={cn(fieldBase, "pr-8 appearance-none cursor-pointer", className)} {...props}>{children}</select>;
}
export function Label({ className, ...props }: React.LabelHTMLAttributes<HTMLLabelElement>) {
  return <label className={cn("block text-[13px] font-medium text-secondary mb-1", className)} {...props} />;
}

/* ----------------------------- Tabs ----------------------------- */
export function Tabs({ tabs, value, onChange }: { tabs: { key: string; label: string }[]; value: string; onChange: (k: string) => void }) {
  return (
    <div className="flex gap-1 border-b border-subtle">
      {tabs.map((t) => (
        <button
          key={t.key}
          onClick={() => onChange(t.key)}
          className={cn(
            "relative px-3 py-2 text-[14px] font-medium -mb-px border-b-2 transition-colors cursor-pointer",
            value === t.key ? "border-accent text-fg" : "border-transparent text-muted hover:text-fg",
          )}
        >
          {t.label}
        </button>
      ))}
    </div>
  );
}

/* ----------------------------- StageProgress (segmented pills) ----------------------------- */
export function StageProgress({ stages, current, onSelect }: { stages: string[]; current: string; onSelect?: (stage: string) => void }) {
  const idx = stages.indexOf(current);
  return (
    <div className="flex items-center gap-1.5 overflow-x-auto pb-1">
      {stages.map((s, i) => {
        const done = idx >= 0 && i < idx;
        const active = i === idx;
        const cls = cn(
          "flex items-center gap-1 whitespace-nowrap rounded-full px-3 py-1 text-[12px] font-medium border",
          active && "bg-accent text-on-accent border-transparent",
          done && "bg-sunken text-secondary border-subtle",
          !active && !done && "bg-card text-muted border-line",
          onSelect && !active && "cursor-pointer hover:border-accent hover:text-fg",
        );
        const inner = <>{done && <Check size={13} />}{s}</>;
        return onSelect ? (
          <button key={s} type="button" disabled={active} onClick={() => onSelect(s)} className={cls}>{inner}</button>
        ) : (
          <div key={s} className={cls}>{inner}</div>
        );
      })}
    </div>
  );
}

/* ----------------------------- Dialog (lightweight modal) ----------------------------- */
export function Dialog({ open, onClose, title, children, footer, wide }: {
  open: boolean; onClose: () => void; title: string; children: React.ReactNode; footer?: React.ReactNode; wide?: boolean;
}) {
  React.useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => { if (e.key === "Escape") onClose(); };
    document.addEventListener("keydown", onKey);
    document.body.style.overflow = "hidden";
    return () => { document.removeEventListener("keydown", onKey); document.body.style.overflow = ""; };
  }, [open, onClose]);
  const [mounted, setMounted] = React.useState(false);
  React.useEffect(() => setMounted(true), []);
  if (!open || !mounted) return null;
  return createPortal(
    <div className="fixed inset-0 z-50 flex items-start justify-center p-4 sm:p-8 overflow-y-auto"
      style={{ background: "var(--surface-overlay)", backdropFilter: "blur(2px)" }}
      onMouseDown={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div className={cn("bg-card border border-subtle rounded-[16px] shadow-[var(--shadow-xl)] w-full mt-8 mb-8", wide ? "max-w-3xl" : "max-w-xl")}
        style={{ animation: "pgz-in 180ms cubic-bezier(0.16,1,0.3,1)" }}>
        <div className="flex items-center justify-between px-5 h-14 border-b border-subtle">
          <h2 className="text-[18px] font-semibold">{title}</h2>
          <Button variant="ghost" size="icon" onClick={onClose} aria-label="Close"><X size={18} /></Button>
        </div>
        <div className="px-5 py-4">{children}</div>
        {footer && <div className="flex items-center justify-end gap-2 px-5 h-16 border-t border-subtle">{footer}</div>}
      </div>
      <style>{`@keyframes pgz-in{from{opacity:0;transform:translateY(8px) scale(.985)}to{opacity:1;transform:none}}`}</style>
    </div>,
    document.body,
  );
}
