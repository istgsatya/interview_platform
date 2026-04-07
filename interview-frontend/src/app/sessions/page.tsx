"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { motion } from "framer-motion";
import { CalendarClock, ChevronRight, Gauge, ListChecks } from "lucide-react";

import { AnimatedMeshBackground } from "@/components/design/animated-mesh-background";
import { Button } from "@/components/ui/button";
import { listSessions, type SessionSummary } from "@/lib/api-client";

export default function SessionsPage() {
  const [sessions, setSessions] = useState<SessionSummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const router = useRouter();

  useEffect(() => {
    let mounted = true;
    listSessions()
      .then((data) => {
        if (!mounted) return;
        setSessions(data);
      })
      .catch(() => {
        if (!mounted) return;
        setError("Could not load sessions.");
      })
      .finally(() => {
        if (!mounted) return;
        setIsLoading(false);
      });

    return () => {
      mounted = false;
    };
  }, []);

  const content = useMemo(() => {
    if (isLoading) return <p className="text-slate-300/80">Loading session history...</p>;
    if (error) return <p className="text-rose-300">{error}</p>;
    if (!sessions.length) return <p className="text-slate-300/80">No sessions yet. Start one from dashboard.</p>;

    return (
      <div className="space-y-3">
        {sessions.map((session, index) => (
          <motion.button
            key={session.sessionId}
            initial={{ opacity: 0, y: 14 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.03 * index }}
            onClick={() => router.push(`/sessions/${session.sessionId}`)}
            className="glass-panel w-full rounded-2xl border border-slate-500/30 p-4 text-left transition hover:border-sky-400/35"
          >
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p className="text-sm text-slate-300">Session #{session.sessionId}</p>
                <p className="text-lg font-semibold text-slate-100">{session.status}</p>
              </div>
              <ChevronRight className="size-4 text-slate-300" />
            </div>

            <div className="mt-3 grid gap-2 text-sm text-slate-300 sm:grid-cols-3">
              <div className="inline-flex items-center gap-1.5"><CalendarClock className="size-4" />{new Date(session.createdAt).toLocaleString()}</div>
              <div className="inline-flex items-center gap-1.5"><ListChecks className="size-4" />{session.answeredCount}/{session.questionCount} answered</div>
              <div className="inline-flex items-center gap-1.5"><Gauge className="size-4" />Avg score: {session.averageScore.toFixed(2)}</div>
            </div>
          </motion.button>
        ))}
      </div>
    );
  }, [error, isLoading, router, sessions]);

  return (
    <div className="relative min-h-screen overflow-hidden px-4 py-8 sm:px-6">
      <AnimatedMeshBackground />
      <div className="relative z-10 mx-auto max-w-5xl">
        <div className="mb-4 flex items-center justify-between gap-3">
          <h1 className="text-2xl font-semibold text-slate-100">Interview Session History</h1>
          <Button variant="secondary" onClick={() => router.push("/dashboard")}>Back to Dashboard</Button>
        </div>
        {content}
      </div>
    </div>
  );
}
