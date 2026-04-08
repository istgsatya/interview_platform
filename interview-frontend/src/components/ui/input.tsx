import * as React from "react";

import { cn } from "@/lib/utils";

function Input({ className, type = "text", ...props }: React.ComponentProps<"input">) {
  return (
    <input
      type={type}
      data-slot="input"
      className={cn(
        "flex h-11 w-full rounded-xl border border-slate-600/45 bg-slate-900/45 px-3 py-2 text-sm text-slate-100 shadow-sm transition outline-none placeholder:text-slate-400/80 focus-visible:border-sky-400/80 focus-visible:ring-2 focus-visible:ring-sky-400/30",
        className,
      )}
      {...props}
    />
  );
}

export { Input };
