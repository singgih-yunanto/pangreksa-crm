"use client";

import * as React from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth";
import { cn } from "@/lib/cn";

export default function SettingsLayout({ children }: { children: React.ReactNode }) {
  const { has, ready } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const canUsers = has("ADMIN_USERS");
  const canRoles = has("ADMIN_ROLES");

  React.useEffect(() => {
    if (ready && !canUsers && !canRoles) router.replace("/");
  }, [ready, canUsers, canRoles, router]);

  if (!ready || (!canUsers && !canRoles)) return null;

  const tabs = [
    ...(canUsers ? [{ href: "/settings/users", label: "Users" }] : []),
    ...(canRoles ? [{ href: "/settings/roles", label: "Roles" }] : []),
  ];

  return (
    <div className="flex flex-col gap-5">
      <div>
        <p className="eyebrow">Administration</p>
        <h1 className="text-[26px] leading-tight">Settings</h1>
      </div>
      <div className="flex gap-1 border-b border-subtle">
        {tabs.map((t) => {
          const active = pathname.startsWith(t.href);
          return (
            <Link key={t.href} href={t.href}
              className={cn("relative px-3 py-2 text-[14px] font-medium -mb-px border-b-2 transition-colors",
                active ? "border-accent text-fg" : "border-transparent text-muted hover:text-fg")}>
              {t.label}
            </Link>
          );
        })}
      </div>
      {children}
    </div>
  );
}
