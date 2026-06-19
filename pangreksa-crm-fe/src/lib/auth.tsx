"use client";

import * as React from "react";
import { clearToken, fetchConfig, fetchMe, getToken, login as apiLogin, setToken, type Me } from "@/lib/api";
import { setMoneyConfig } from "@/lib/format";

type AuthState = {
  user: Me | null;
  ready: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  has: (perm: string) => boolean;
};

const Ctx = React.createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = React.useState<Me | null>(null);
  const [ready, setReady] = React.useState(false);

  const loadConfig = React.useCallback(() => {
    fetchConfig().then(setMoneyConfig).catch(() => {});
  }, []);

  React.useEffect(() => {
    if (!getToken()) { setReady(true); return; }
    fetchMe()
      .then((m) => { setUser(m); loadConfig(); })
      .catch(() => clearToken())
      .finally(() => setReady(true));
  }, [loadConfig]);

  const login = async (username: string, password: string) => {
    const { token, user } = await apiLogin(username, password);
    setToken(token);
    setUser(user);
    loadConfig();
  };

  const logout = () => {
    clearToken();
    setUser(null);
    if (typeof window !== "undefined") location.assign("/login");
  };

  const has = (perm: string) => !!user?.permissions.includes(perm);

  return <Ctx.Provider value={{ user, ready, login, logout, has }}>{children}</Ctx.Provider>;
}

export function useAuth(): AuthState {
  const ctx = React.useContext(Ctx);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
