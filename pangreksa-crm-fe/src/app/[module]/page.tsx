"use client";

import { Suspense } from "react";
import { useParams } from "next/navigation";
import { MODULES } from "@/lib/modules";
import { ModuleScreen } from "@/components/module-screen";

export default function Page() {
  const { module } = useParams<{ module: string }>();
  if (!MODULES[module]) return <div className="text-muted text-[14px]">Unknown module.</div>;
  return (
    <Suspense fallback={<div className="text-muted text-[14px]">Loading…</div>}>
      <ModuleScreen moduleKey={module} />
    </Suspense>
  );
}
