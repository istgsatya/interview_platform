"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { motion } from "framer-motion";
import axios from "axios";
import { ArrowRight, ClipboardList, FileText, History, Rocket } from "lucide-react";

import { AnimatedMeshBackground } from "@/components/design/animated-mesh-background";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { startInterview } from "@/lib/api-client";

export default function DashboardPage() {
  const [resume, setResume] = useState("");
  const [jobDescription, setJobDescription] = useState("");
  const [isLaunching, setIsLaunching] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const router = useRouter();

  const canStart = resume.trim().length > 40 && jobDescription.trim().length > 40;

  async function handleStartInterview() {
    if (!canStart || isLaunching) return;
    setError(null);
    setIsLaunching(true);

    try {
      const result = await startInterview({
        resumeText: resume.trim(),
        targetJdText: jobDescription.trim(),
      });
      await new Promise((resolve) => setTimeout(resolve, 280));
      router.push(`/interview/${result.sessionId}`);
    } catch (err: unknown) {
      if (axios.isAxiosError(err) && typeof err.response?.data === "string") {
        setError(err.response.data);
      } else {
        setError("Could not start interview. Verify backend is up and JWT is valid.");
      }
      setIsLaunching(false);
    }
  }

  return (
    <div className="relative min-h-screen overflow-hidden px-4 py-7 sm:px-6 sm:py-10">
      <AnimatedMeshBackground />

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.45 }}
        className="relative z-10 mx-auto max-w-6xl"
      >
        <header className="mb-6 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-600/35 bg-slate-950/45 px-5 py-4 backdrop-blur-xl">
          <div>
            <p className="text-xs uppercase tracking-[0.18em] text-slate-400">AI Interview Gateway</p>
            <h1 className="text-2xl font-semibold text-slate-100">Interview Command Center</h1>
          </div>
          <div className="rounded-full border border-sky-400/35 bg-sky-500/12 px-3 py-1 text-xs text-sky-200">
            Session Prep
          </div>
        </header>

        <div className="grid gap-5 lg:grid-cols-2">
          <motion.section
            whileInView={{ opacity: 1, y: 0 }}
            initial={{ opacity: 0, y: 16 }}
            transition={{ duration: 0.35, delay: 0.05 }}
            viewport={{ once: true, margin: "-60px" }}
            className="glass-panel rounded-3xl p-5 sm:p-6"
          >
            <div className="mb-3 inline-flex items-center gap-2 text-slate-200">
              <FileText className="size-4 text-cyan-300" />
              <h2 className="text-lg font-semibold">Candidate Resume</h2>
            </div>
            <Label htmlFor="resume" className="mb-2 block text-slate-300">Paste resume text</Label>
            <Textarea
              id="resume"
              value={resume}
              onChange={(e) => setResume(e.target.value)}
              placeholder="Example: 2 years of Java/Spring Boot, PostgreSQL, REST APIs, CI/CD..."
              className="min-h-64"
            />
          </motion.section>

          <motion.section
            whileInView={{ opacity: 1, y: 0 }}
            initial={{ opacity: 0, y: 16 }}
            transition={{ duration: 0.35, delay: 0.12 }}
            viewport={{ once: true, margin: "-60px" }}
            className="glass-panel rounded-3xl p-5 sm:p-6"
          >
            <div className="mb-3 inline-flex items-center gap-2 text-slate-200">
              <ClipboardList className="size-4 text-violet-300" />
              <h2 className="text-lg font-semibold">Target Job Description</h2>
            </div>
            <Label htmlFor="jd" className="mb-2 block text-slate-300">Paste JD text</Label>
            <Textarea
              id="jd"
              value={jobDescription}
              onChange={(e) => setJobDescription(e.target.value)}
              placeholder="Example: Senior backend engineer with microservices, AWS, Kubernetes, and system design expertise..."
              className="min-h-64"
            />
          </motion.section>
        </div>

        <motion.footer
          initial={{ opacity: 0, y: 18 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4, delay: 0.16 }}
          className="mt-6 rounded-3xl border border-slate-600/35 bg-slate-950/45 p-5 backdrop-blur-xl"
        >
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="text-sm text-slate-300">
              {canStart
                ? "Ready to launch. Candidate profile and target role context look good."
                : "Add enough detail in both textareas (minimum 40 chars each) to enable interview launch."}
            </div>
            <div className="flex flex-wrap items-center gap-2">
              <Button
                variant="secondary"
                size="lg"
                onClick={() => router.push("/sessions")}
                className="h-14 rounded-2xl px-6"
              >
                <History className="size-4" />
                Session History
              </Button>

              <Button
                size="lg"
                onClick={handleStartInterview}
                disabled={!canStart || isLaunching}
                className="h-14 rounded-2xl px-8 text-base shadow-[0_0_45px_rgba(56,189,248,0.45)]"
              >
                <Rocket className={`size-4 ${isLaunching ? "animate-pulse" : ""}`} />
                {isLaunching ? "Launching Interview..." : "Start Interview"}
                <ArrowRight className="size-4" />
              </Button>
            </div>
          </div>

          {error ? (
            <p className="mt-3 rounded-xl border border-rose-400/30 bg-rose-500/12 px-3 py-2 text-sm text-rose-200">
              {error}
            </p>
          ) : null}
        </motion.footer>
      </motion.div>
    </div>
  );
}
