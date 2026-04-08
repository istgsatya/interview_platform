"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useParams } from "next/navigation";
import { AnimatePresence, motion } from "framer-motion";
import { Bot, Circle, Cpu, Download, SendHorizontal, Sparkles, User2 } from "lucide-react";

import { AnimatedMeshBackground } from "@/components/design/animated-mesh-background";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { getInterviewTranscript } from "@/lib/api-client";

type MessageKind = "user" | "question" | "score" | "system";

type ChatMessage = {
  id: string;
  text: string;
  kind: MessageKind;
  score?: number;
};

function classifyServerMessage(text: string): ChatMessage {
  const cleaned = text.trim();

  if (cleaned.includes("QUESTION") || cleaned.startsWith("QUESTION") || cleaned.startsWith("\nQUESTION")) {
    return {
      id: crypto.randomUUID(),
      text: cleaned,
      kind: "question",
    };
  }

  if (cleaned.startsWith(">> AI EVALUATION:")) {
    const match = cleaned.match(/AI EVALUATION:\s*([\d.]+)\/10/i);
    return {
      id: crypto.randomUUID(),
      text: cleaned,
      kind: "score",
      score: match ? Number(match[1]) : undefined,
    };
  }

  return {
    id: crypto.randomUUID(),
    text: cleaned,
    kind: "system",
  };
}

function scoreGlow(score?: number): string {
  if (score === undefined) return "border-violet-400/45 bg-violet-500/10";
  if (score >= 7) return "border-emerald-400/50 bg-emerald-500/12 shadow-[0_0_24px_rgba(16,185,129,0.35)]";
  if (score >= 4) return "border-amber-400/50 bg-amber-500/12 shadow-[0_0_24px_rgba(245,158,11,0.35)]";
  return "border-rose-400/55 bg-rose-500/12 shadow-[0_0_24px_rgba(244,63,94,0.35)]";
}

export default function InterviewRoomPage() {
  const { sessionId } = useParams<{ sessionId: string }>();
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [draft, setDraft] = useState("");
  const [isConnected, setIsConnected] = useState(false);
  const [isEvaluating, setIsEvaluating] = useState(false);
  const [connectionError, setConnectionError] = useState<string | null>(null);
  const [isExporting, setIsExporting] = useState(false);
  const [isSessionComplete, setIsSessionComplete] = useState(false);
  const socketRef = useRef<WebSocket | null>(null);
  const scrollRef = useRef<HTMLDivElement | null>(null);

  const title = useMemo(() => `Interview Session #${sessionId}`, [sessionId]);

  useEffect(() => {
    const ws = new WebSocket("ws://localhost:8080/ws/interview");
    socketRef.current = ws;

    ws.onopen = () => {
      setIsConnected(true);
      setConnectionError(null);
      ws.send(`START:${sessionId}`);
      setMessages((prev) => [
        ...prev,
        {
          id: crypto.randomUUID(),
          kind: "system",
          text: `Connected to AI Gateway. Bootstrapping session ${sessionId}...`,
        },
      ]);
    };

    ws.onmessage = (event) => {
      const incoming = String(event.data ?? "");
      const parsed = classifyServerMessage(incoming);
      setMessages((prev) => [...prev, parsed]);

      if (incoming.includes("Evaluating your answer")) {
        setIsEvaluating(true);
      }
      if (incoming.startsWith(">> AI EVALUATION") || incoming.includes("Interview Complete")) {
        setIsEvaluating(false);
      }
      if (incoming.includes("Interview Complete")) {
        setIsSessionComplete(true);
      }
    };

    ws.onclose = () => {
      setIsConnected(false);
      setIsEvaluating(false);
      setMessages((prev) => [
        ...prev,
        {
          id: crypto.randomUUID(),
          kind: "system",
          text: "Connection closed. Refresh to reconnect.",
        },
      ]);
    };

    ws.onerror = () => {
      setConnectionError("WebSocket error: ensure backend is running on ws://localhost:8080/ws/interview");
      setIsConnected(false);
      setIsEvaluating(false);
    };

    return () => {
      ws.close();
    };
  }, [sessionId]);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTo({ top: scrollRef.current.scrollHeight, behavior: "smooth" });
    }
  }, [messages, isEvaluating]);

  function sendAnswer() {
    const payload = draft.trim();
    if (!payload || !socketRef.current || socketRef.current.readyState !== WebSocket.OPEN) {
      return;
    }

    socketRef.current.send(`ANSWER:${payload}`);
    setMessages((prev) => [
      ...prev,
      {
        id: crypto.randomUUID(),
        kind: "user",
        text: payload,
      },
    ]);
    setDraft("");
    setIsEvaluating(true);
  }

  async function exportTranscript() {
    setIsExporting(true);
    try {
      const transcript = await getInterviewTranscript(sessionId);
      const blob = new Blob([JSON.stringify(transcript, null, 2)], {
        type: "application/json;charset=utf-8",
      });
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = `interview-session-${sessionId}-transcript.json`;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      URL.revokeObjectURL(url);
    } catch {
      setConnectionError("Could not export transcript yet. Ensure session exists and API is reachable.");
    } finally {
      setIsExporting(false);
    }
  }

  return (
    <div className="relative min-h-screen overflow-hidden px-3 py-5 sm:px-6 sm:py-7">
      <AnimatedMeshBackground />

      <div className="relative z-10 mx-auto flex h-[calc(100vh-2.5rem)] w-full max-w-6xl flex-col overflow-hidden rounded-3xl border border-slate-500/30 bg-slate-950/50 backdrop-blur-2xl">
        <div className="flex items-center justify-between border-b border-slate-700/45 px-5 py-4">
          <div>
            <p className="text-xs uppercase tracking-[0.2em] text-slate-400">Realtime AI Interview</p>
            <h1 className="text-lg font-semibold text-slate-100 sm:text-xl">{title}</h1>
          </div>
          <div className="inline-flex items-center gap-2 rounded-full border border-slate-600/40 bg-slate-900/65 px-3 py-1.5 text-xs text-slate-200">
            <Circle className={`size-2 ${isConnected ? "fill-emerald-400 text-emerald-400" : "fill-rose-400 text-rose-400"}`} />
            {isConnected ? "Connected" : "Disconnected"}
          </div>
        </div>

        <div className="flex justify-end border-b border-slate-700/45 px-5 py-3">
          <Button
            variant="secondary"
            size="sm"
            onClick={exportTranscript}
            disabled={isExporting}
            className="rounded-xl"
          >
            <Download className="size-4" />
            {isExporting ? "Exporting..." : "Export Transcript"}
          </Button>
        </div>

        <div ref={scrollRef} className="relative flex-1 space-y-3 overflow-y-auto px-4 py-4 sm:px-6">
          <AnimatePresence initial={false}>
            {messages.map((msg) => {
              const isUser = msg.kind === "user";
              const isScore = msg.kind === "score";
              const isQuestion = msg.kind === "question";

              return (
                <motion.div
                  key={msg.id}
                  initial={{ opacity: 0, x: isUser ? 24 : -24, y: 8 }}
                  animate={{ opacity: 1, x: 0, y: 0 }}
                  exit={{ opacity: 0 }}
                  transition={{ duration: 0.26, ease: "easeOut" }}
                  className={`flex ${isUser ? "justify-end" : "justify-start"}`}
                >
                  <div
                    className={[
                      "max-w-[86%] rounded-2xl border px-4 py-3 text-sm leading-relaxed sm:max-w-[78%]",
                      isUser
                        ? "border-sky-400/45 bg-sky-500/18 text-sky-50 shadow-[0_10px_30px_rgba(14,165,233,0.18)]"
                        : isScore
                          ? scoreGlow(msg.score)
                          : isQuestion
                            ? "border-violet-400/45 bg-violet-500/12 text-violet-50"
                            : "border-slate-600/45 bg-slate-900/65 text-slate-200",
                    ].join(" ")}
                  >
                    <div className="mb-1 inline-flex items-center gap-1.5 text-[11px] uppercase tracking-[0.16em] text-slate-300/75">
                      {isUser ? <User2 className="size-3.5" /> : isScore ? <Sparkles className="size-3.5" /> : <Bot className="size-3.5" />}
                      {isUser ? "You" : isScore ? "AI Evaluation" : isQuestion ? "Question" : "System"}
                    </div>
                    <p className="whitespace-pre-wrap">{msg.text}</p>
                  </div>
                </motion.div>
              );
            })}
          </AnimatePresence>

          <AnimatePresence>
            {isEvaluating && (
              <motion.div
                initial={{ opacity: 0, x: -14 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -6 }}
                className="flex justify-start"
              >
                <div className="inline-flex items-center gap-2 rounded-2xl border border-emerald-400/35 bg-emerald-500/10 px-4 py-2.5 text-sm text-emerald-100">
                  <Cpu className="size-4 animate-pulse" />
                  AI is evaluating your answer...
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        <div className="border-t border-slate-700/45 px-4 py-4 sm:px-6">
          {connectionError ? (
            <p className="mb-3 rounded-xl border border-rose-400/30 bg-rose-500/12 px-3 py-2 text-sm text-rose-200">{connectionError}</p>
          ) : null}

          {isSessionComplete ? (
            <p className="mb-3 rounded-xl border border-emerald-400/35 bg-emerald-500/12 px-3 py-2 text-sm text-emerald-200">
              Session complete. You can export your transcript now.
            </p>
          ) : null}

          <div className="flex items-center gap-2 sm:gap-3">
            <Input
              value={draft}
              onChange={(event) => setDraft(event.target.value)}
              placeholder="Type your answer and press Enter…"
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  event.preventDefault();
                  sendAnswer();
                }
              }}
              className="h-12 rounded-2xl"
            />
            <Button onClick={sendAnswer} size="lg" className="h-12 rounded-2xl px-5">
              <SendHorizontal className="size-4" />
              Send
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
