"use client";

import * as React from "react";
import { usePathname, useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth";
import { AppShell } from "@/components/app-shell";

export function AppGate({ children }: { children: React.ReactNode }) {
  const { user, ready } = useAuth();
  const pathname = usePathname();
  const router = useRouter();

  const isLogin = pathname?.startsWith("/login");

  React.useEffect(() => {
    if (ready && !user && !isLogin) router.replace("/login");
  }, [ready, user, isLogin, router]);

  if (isLogin) return <>{children}</>;
  if (!ready) return <div className="min-h-screen grid place-items-center text-muted text-[14px]">Loading…</div>;
  if (!user) return null;
  return <AppShell>{children}</AppShell>;
}
