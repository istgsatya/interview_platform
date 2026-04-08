"use client";

import type { ReactNode } from "react";
import Link from "next/link";
import { motion } from "framer-motion";
import { ArrowUpRight } from "lucide-react";

import { AnimatedMeshBackground } from "@/components/design/animated-mesh-background";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

type AuthShellProps = {
  title: string;
  description: string;
  alternateHref: string;
  alternateLabel: string;
  alternateText: string;
  children: ReactNode;
};

export function AuthShell({
  title,
  description,
  alternateHref,
  alternateLabel,
  alternateText,
  children,
}: AuthShellProps) {
  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden px-4 py-10 sm:px-6">
      <AnimatedMeshBackground />

      <motion.div
        initial={{ opacity: 0, y: 24, scale: 0.98 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        transition={{ duration: 0.55, ease: [0.16, 1, 0.3, 1] }}
        className="relative z-10 w-full max-w-lg"
      >
        <Card>
          <CardHeader>
            <motion.span
              className="inline-flex w-fit rounded-full border border-sky-400/30 bg-sky-500/10 px-3 py-1 text-xs font-medium text-sky-300"
              initial={{ opacity: 0, x: -8 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.1 }}
            >
              AI Interview Gateway
            </motion.span>
            <CardTitle>{title}</CardTitle>
            <CardDescription>{description}</CardDescription>
          </CardHeader>

          <div className="px-7 pb-3">{children}</div>

          <div className="flex flex-wrap items-center justify-between gap-3 border-t border-slate-600/30 px-7 py-5 text-sm text-slate-300/80">
            <span>{alternateText}</span>
            <Link
              href={alternateHref}
              className="group inline-flex items-center gap-1 font-medium text-sky-300 transition hover:text-sky-200"
            >
              {alternateLabel}
              <ArrowUpRight className="size-4 transition group-hover:translate-x-0.5 group-hover:-translate-y-0.5" />
            </Link>
          </div>
        </Card>
      </motion.div>
    </div>
  );
}
