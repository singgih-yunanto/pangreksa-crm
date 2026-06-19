"use client";

import { useParams } from "next/navigation";
import { MODULES } from "@/lib/modules";
import { RecordDetail } from "@/components/record-detail";

export default function Page() {
  const { module, id } = useParams<{ module: string; id: string }>();
  if (!MODULES[module]) return <div className="text-muted text-[14px]">Unknown module.</div>;
  return <RecordDetail moduleKey={module} id={id} />;
}
