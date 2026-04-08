import * as React from "react";

import { cn } from "@/lib/utils";

function Textarea({ className, ...props }: React.ComponentProps<"textarea">) {
  return (
    <textarea
      data-slot="textarea"
      className={cn(
        "flex min-h-44 w-full rounded-2xl border border-slate-600/45 bg-slate-900/45 px-4 py-3 text-sm text-slate-100 shadow-sm transition outline-none placeholder:text-slate-400/80 focus-visible:border-sky-400/80 focus-visible:ring-2 focus-visible:ring-sky-400/30",
        className,
      )}
      {...props}
    />
  );
}

export { Textarea };
