"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { motion } from "framer-motion";
import { Bot, Download, User2 } from "lucide-react";

import { AnimatedMeshBackground } from "@/components/design/animated-mesh-background";
import { Button } from "@/components/ui/button";
import { getInterviewTranscript, type TranscriptResponse } from "@/lib/api-client";

export default function SessionDetailsPage() {
  const { sessionId } = useParams<{ sessionId: string }>();
  const router = useRouter();

  const [data, setData] = useState<TranscriptResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;
    getInterviewTranscript(sessionId)
      .then((result) => {
        if (!mounted) return;
        setData(result);
      })
      .catch(() => {
        if (!mounted) return;
        setError("Could not load transcript for this session.");
      })
      .finally(() => {
        if (!mounted) return;
        setIsLoading(false);
      });

    return () => {
      mounted = false;
    };
  }, [sessionId]);

  function exportTranscript() {
    if (!data) return;
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = `interview-session-${sessionId}.json`;
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    URL.revokeObjectURL(url);
  }

  return (
    <div className="relative min-h-screen overflow-hidden px-4 py-8 sm:px-6">
      <AnimatedMeshBackground />
      <div className="relative z-10 mx-auto max-w-5xl">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
          <h1 className="text-2xl font-semibold text-slate-100">Session Replay #{sessionId}</h1>
          <div className="flex items-center gap-2">
            <Button variant="secondary" onClick={() => router.push("/sessions")}>Back</Button>
            <Button onClick={exportTranscript} disabled={!data}><Download className="size-4" />Export</Button>
          </div>
        </div>

        {isLoading ? <p className="text-slate-300/80">Loading transcript...</p> : null}
        {error ? <p className="text-rose-300">{error}</p> : null}

        {data ? (
          <div className="space-y-3">
            {data.items.map((item, index) => (
              <motion.div
                key={item.questionId}
                initial={{ opacity: 0, y: 14 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.03 * index }}
                className="glass-panel rounded-2xl border border-slate-500/30 p-4"
              >
                <div className="mb-2 inline-flex items-center gap-1.5 text-xs uppercase tracking-[0.16em] text-violet-300">
                  <Bot className="size-3.5" />
                  Question L{item.difficultyLevel}
                </div>
                <p className="text-slate-100">{item.questionText}</p>

                <div className="mt-3 rounded-xl border border-slate-600/40 bg-slate-900/40 p-3">
                  <div className="mb-1 inline-flex items-center gap-1.5 text-xs uppercase tracking-[0.16em] text-sky-300">
                    <User2 className="size-3.5" />
                    Candidate Answer
                  </div>
                  <p className="text-sm text-slate-200">{item.candidateText ?? "(Not answered yet)"}</p>
                </div>

                <div className="mt-3 grid gap-2 text-xs text-slate-300 sm:grid-cols-3">
                  <div>Score: {item.finalAggregateScore ?? 0}</div>
                  <div>Similarity: {item.mlSimilarityScore ?? 0}</div>
                  <div>Length Ratio: {item.mlLengthScore ?? 0}</div>
                </div>
                {item.llmFeedback ? <p className="mt-2 text-sm text-emerald-200">{item.llmFeedback}</p> : null}
              </motion.div>
            ))}
          </div>
        ) : null}
      </div>
    </div>
  );
}
