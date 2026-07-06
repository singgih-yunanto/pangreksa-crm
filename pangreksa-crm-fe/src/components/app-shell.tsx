"use client";

import * as React from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { Command } from "cmdk";
import {
  Handshake, UserPlus, Building2, Users, LayoutDashboard, Settings, Plus, Bell, Search, Sun, Moon, Shield,
  LogOut, CheckSquare, CalendarDays, Phone, type LucideIcon,
} from "lucide-react";
import { MODULES, MODULE_ORDER } from "@/lib/modules";
import { useAuth } from "@/lib/auth";
import { cn } from "@/lib/cn";
import { Button, InitialChip } from "@/components/ui";

const ICONS: Record<string, LucideIcon> = { Handshake, UserPlus, Building2, Users, LayoutDashboard, Settings, CheckSquare, CalendarDays, Phone };

type NavItem = { key: string; href: string; label: string; icon: string };

function useNav(): NavItem[] {
  const { has } = useAuth();
  const items: NavItem[] = [{ key: "home", href: "/", label: "Home", icon: "LayoutDashboard" }];
  for (const k of MODULE_ORDER) {
    const m = MODULES[k];
    if (has(`${m.perm}_VIEW`)) items.push({ key: k, href: `/${k}`, label: m.plural, icon: m.icon });
  }
  if (has("ADMIN_USERS") || has("ADMIN_ROLES")) {
    items.push({ key: "settings", href: "/settings/users", label: "Settings", icon: "Settings" });
  }
  return items;
}

function useTheme() {
  const [dark, setDark] = React.useState(false);
  React.useEffect(() => setDark(document.documentElement.classList.contains("dark")), []);
  const toggle = () => {
    const next = !document.documentElement.classList.contains("dark");
    document.documentElement.classList.toggle("dark", next);
    localStorage.setItem("pangreksa-theme", next ? "dark" : "light");
    setDark(next);
  };
  return { dark, toggle };
}

function Rail({ nav, pathname }: { nav: NavItem[]; pathname: string }) {
  return (
    <aside className="group fixed left-0 top-0 bottom-0 z-30 w-14 hover:w-[232px] transition-[width] duration-200 bg-card border-r border-subtle flex flex-col">
      <div className="h-14 flex items-center gap-2 px-3.5 border-b border-subtle overflow-hidden">
        <Shield className="text-accent shrink-0" size={22} />
        <span className="font-semibold text-[15px] whitespace-nowrap opacity-0 group-hover:opacity-100 transition-opacity" style={{ fontFamily: "var(--font-bricolage)" }}>Pangreksa CRM</span>
      </div>
      <nav className="flex-1 py-2 px-2 flex flex-col gap-0.5">
        {nav.map((n) => {
          const Icon = ICONS[n.icon] ?? LayoutDashboard;
          const active = n.href === "/" ? pathname === "/" : pathname.startsWith(n.href.split("/").slice(0, 2).join("/"));
          return (
            <Link key={n.key} href={n.href}
              className={cn("flex items-center gap-3 h-9 rounded-[8px] px-2.5 transition-colors overflow-hidden",
                active ? "bg-accent-soft text-accent-soft-fg" : "text-secondary hover:bg-hover hover:text-fg")}>
              <Icon size={20} className="shrink-0" />
              <span className="text-[14px] font-medium whitespace-nowrap opacity-0 group-hover:opacity-100 transition-opacity">{n.label}</span>
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}

function CommandPalette({ nav, open, setOpen }: { nav: NavItem[]; open: boolean; setOpen: (b: boolean) => void }) {
  const router = useRouter();
  const { has } = useAuth();
  const go = (href: string) => { setOpen(false); router.push(href); };
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center p-4 pt-[12vh]"
      style={{ background: "var(--surface-overlay)", backdropFilter: "blur(2px)" }}
      onMouseDown={(e) => { if (e.target === e.currentTarget) setOpen(false); }}>
      <Command className="w-full max-w-xl bg-card border border-subtle rounded-[16px] shadow-[var(--shadow-xl)] overflow-hidden"
        style={{ animation: "pgz-in 180ms cubic-bezier(0.16,1,0.3,1)" }}>
        <div className="flex items-center gap-2 px-4 h-12 border-b border-subtle">
          <Search size={16} className="text-muted" />
          <Command.Input autoFocus placeholder="Search or run a command…" className="flex-1 bg-transparent outline-none text-[14px]" />
          <kbd className="tabular text-[11px] text-muted border border-subtle rounded px-1.5 py-0.5">ESC</kbd>
        </div>
        <Command.List className="max-h-80 overflow-y-auto p-2">
          <Command.Empty className="px-3 py-6 text-center text-[13px] text-muted">No results.</Command.Empty>
          <Command.Group heading="Go to" className="[&_[cmdk-group-heading]]:eyebrow [&_[cmdk-group-heading]]:px-2 [&_[cmdk-group-heading]]:py-1">
            {nav.map((n) => (
              <Command.Item key={n.key} value={`go ${n.label}`} onSelect={() => go(n.href)}
                className="flex items-center gap-2 px-2 py-2 rounded-[8px] text-[14px] cursor-pointer data-[selected=true]:bg-hover">
                {React.createElement(ICONS[n.icon] ?? LayoutDashboard, { size: 16, className: "text-muted" })}
                {n.label}
              </Command.Item>
            ))}
          </Command.Group>
          <Command.Group heading="Create" className="[&_[cmdk-group-heading]]:eyebrow [&_[cmdk-group-heading]]:px-2 [&_[cmdk-group-heading]]:py-1">
            {MODULE_ORDER.filter((k) => has(`${MODULES[k].perm}_CREATE`)).map((k) => (
              <Command.Item key={k} value={`new ${MODULES[k].singular}`} onSelect={() => go(`/${k}?new=1`)}
                className="flex items-center gap-2 px-2 py-2 rounded-[8px] text-[14px] cursor-pointer data-[selected=true]:bg-hover">
                <Plus size={16} className="text-muted" /> New {MODULES[k].singular.toLowerCase()}
              </Command.Item>
            ))}
          </Command.Group>
        </Command.List>
      </Command>
      <style>{`@keyframes pgz-in{from{opacity:0;transform:translateY(8px) scale(.985)}to{opacity:1;transform:none}}`}</style>
    </div>
  );
}

function ProfileMenu() {
  const { user, logout } = useAuth();
  const [open, setOpen] = React.useState(false);
  const ref = React.useRef<HTMLDivElement>(null);
  React.useEffect(() => {
    const onClick = (e: MouseEvent) => { if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false); };
    document.addEventListener("mousedown", onClick);
    return () => document.removeEventListener("mousedown", onClick);
  }, []);
  return (
    <div className="relative" ref={ref}>
      <button onClick={() => setOpen((o) => !o)} aria-label="Account" className="cursor-pointer"><InitialChip name={user?.fullName} size={28} /></button>
      {open && (
        <div className="absolute right-0 top-10 w-56 bg-card border border-subtle rounded-[12px] shadow-[var(--shadow-md)] p-1.5 z-50">
          <div className="px-2.5 py-2">
            <p className="text-[14px] font-medium">{user?.fullName}</p>
            <p className="text-[12px] text-muted">{user?.roleName}</p>
          </div>
          <div className="h-px bg-subtle my-1" />
          <button onClick={logout} className="w-full flex items-center gap-2 px-2.5 h-9 rounded-[8px] text-[14px] text-secondary hover:bg-hover hover:text-fg cursor-pointer">
            <LogOut size={16} /> Sign out
          </button>
        </div>
      )}
    </div>
  );
}

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const nav = useNav();
  const { dark, toggle } = useTheme();
  const [paletteOpen, setPaletteOpen] = React.useState(false);

  React.useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === "k") { e.preventDefault(); setPaletteOpen((o) => !o); }
    };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, []);

  return (
    <div className="min-h-screen bg-page">
      <Rail nav={nav} pathname={pathname} />
      <div className="pl-14">
        <header className="sticky top-0 z-20 h-14 bg-card/90 backdrop-blur border-b border-subtle flex items-center gap-3 px-4">
          <button onClick={() => setPaletteOpen(true)}
            className="flex items-center gap-2 h-9 px-3 rounded-[8px] border border-line bg-page text-muted text-[13px] hover:bg-hover w-full max-w-md cursor-pointer">
            <Search size={15} /><span>Search or run a command…</span>
            <kbd className="tabular ml-auto text-[11px] border border-subtle rounded px-1.5 py-0.5">⌘K</kbd>
          </button>
          <div className="ml-auto flex items-center gap-1">
            <Button variant="primary" size="sm" onClick={() => setPaletteOpen(true)}><Plus size={16} /> New</Button>
            <Button variant="ghost" size="icon" aria-label="Notifications"><Bell size={18} /></Button>
            <Button variant="ghost" size="icon" aria-label="Toggle theme" onClick={toggle}>{dark ? <Sun size={18} /> : <Moon size={18} />}</Button>
            <ProfileMenu />
          </div>
        </header>
        <main className="px-6 py-6 mx-auto" style={{ maxWidth: 1440 }}>{children}</main>
      </div>
      <CommandPalette nav={nav} open={paletteOpen} setOpen={setPaletteOpen} />
    </div>
  );
}
