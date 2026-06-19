let MONEY = { currency: "IDR", decimalPlaces: 0, locale: "id-ID" };
export function setMoneyConfig(cfg: { currency: string; decimalPlaces: number; locale: string }) {
  MONEY = cfg;
}
export function money(v: number | null | undefined): string {
  if (v == null) return "—";
  return new Intl.NumberFormat(MONEY.locale, {
    style: "currency", currency: MONEY.currency,
    maximumFractionDigits: MONEY.decimalPlaces, minimumFractionDigits: MONEY.decimalPlaces,
  }).format(v);
}

export function pct(v: number | null | undefined): string {
  return v == null ? "—" : `${v}%`;
}

export function date(v: string | null | undefined): string {
  return v ? v : "—";
}

export function initials(name: string | null | undefined): string {
  if (!name) return "?";
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "?";
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

const AVATAR_COLORS = ["#962829", "#615D5C", "#8C5E12", "#235488", "#1F6B43", "#6B4FB8", "#AF3931"];
export function avatarColor(seed: string | null | undefined): string {
  const s = seed ?? "";
  let h = 0;
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) >>> 0;
  return AVATAR_COLORS[h % AVATAR_COLORS.length];
}
