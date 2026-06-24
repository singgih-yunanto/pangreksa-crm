"use client";

import {
  useInfiniteQuery, useMutation, useQuery, useQueryClient,
} from "@tanstack/react-query";
import {
  createRecord, deleteRecord, fetchConfig, fetchLookups, getRecord, listRecords, updateRecord,
  type Lookup, type Record,
} from "@/lib/api";

const PAGE = 30;

export function useInfiniteList(endpoint: string, q?: string, sort?: string, filters?: { [k: string]: string }) {
  return useInfiniteQuery({
    queryKey: ["list", endpoint, q ?? "", sort ?? "", filters ?? {}],
    initialPageParam: 0,
    queryFn: ({ pageParam }) => listRecords<Record>(endpoint, { offset: pageParam as number, limit: PAGE, q, sort, filters }),
    getNextPageParam: (last, all) => {
      const loaded = all.reduce((n, p) => n + p.items.length, 0);
      return loaded < last.total ? loaded : undefined;
    },
  });
}

/** Loads all records (for kanban — chunked). */
export function useAllRecords(endpoint: string, enabled = true) {
  return useQuery({
    queryKey: ["all", endpoint],
    enabled,
    queryFn: async () => {
      const out: Record[] = [];
      let offset = 0;
      for (;;) {
        const { items, total } = await listRecords<Record>(endpoint, { offset, limit: 100 });
        out.push(...items);
        offset += items.length;
        if (items.length === 0 || out.length >= total) break;
      }
      return out;
    },
  });
}

export function useLookups(category: string | undefined) {
  return useQuery<Lookup[]>({
    queryKey: ["lookups", category],
    enabled: !!category,
    staleTime: Infinity,
    queryFn: () => fetchLookups(category!),
  });
}

export function useConfig() {
  return useQuery({ queryKey: ["config"], queryFn: fetchConfig, staleTime: Infinity });
}

export function useRecord(endpoint: string, id: string | number) {
  return useQuery({ queryKey: ["record", endpoint, String(id)], queryFn: () => getRecord<Record>(endpoint, id) });
}

export function useCreate(endpoint: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: unknown) => createRecord(endpoint, body),
    onSuccess: () => invalidate(qc, endpoint),
  });
}

export function useUpdate(endpoint: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: { id: number | string; body: unknown }) => updateRecord(endpoint, id, body),
    onSuccess: () => invalidate(qc, endpoint),
  });
}

export function useDelete(endpoint: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number | string) => deleteRecord(endpoint, id),
    onSuccess: () => invalidate(qc, endpoint),
  });
}

function invalidate(qc: ReturnType<typeof useQueryClient>, endpoint: string) {
  qc.invalidateQueries({ queryKey: ["list", endpoint] });
  qc.invalidateQueries({ queryKey: ["all", endpoint] });
  qc.invalidateQueries({ queryKey: ["record", endpoint] });
}
