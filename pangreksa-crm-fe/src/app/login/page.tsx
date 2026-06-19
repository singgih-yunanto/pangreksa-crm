"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { Shield } from "lucide-react";
import { useAuth } from "@/lib/auth";
import { ApiError } from "@/lib/api";
import { Button, Card, Input, Label } from "@/components/ui";

export default function LoginPage() {
  const { login, user, ready } = useAuth();
  const router = useRouter();
  const [username, setUsername] = React.useState("");
  const [password, setPassword] = React.useState("");
  const [error, setError] = React.useState("");
  const [busy, setBusy] = React.useState(false);

  React.useEffect(() => { if (ready && user) router.replace("/"); }, [ready, user, router]);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setBusy(true);
    try {
      await login(username.trim(), password);
      router.replace("/");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Couldn't sign in. Try again.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="min-h-screen grid place-items-center bg-page p-4">
      <Card className="w-full max-w-sm p-6">
        <div className="flex items-center gap-2 mb-1">
          <Shield className="text-accent" size={24} />
          <span className="text-[18px] font-semibold" style={{ fontFamily: "var(--font-bricolage)" }}>Pangreksa CRM</span>
        </div>
        <p className="text-[13px] text-muted mb-5">Sign in to continue.</p>
        <form onSubmit={submit} className="flex flex-col gap-3">
          <div>
            <Label htmlFor="username">Username</Label>
            <Input id="username" autoFocus value={username} onChange={(e) => setUsername(e.target.value)} />
          </div>
          <div>
            <Label htmlFor="password">Password</Label>
            <Input id="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
          </div>
          {error && <p role="alert" className="text-[13px] text-danger">{error}</p>}
          <Button type="submit" variant="primary" disabled={busy} className="mt-1 w-full">
            {busy ? "Signing in…" : "Sign in"}
          </Button>
        </form>
      </Card>
    </div>
  );
}
