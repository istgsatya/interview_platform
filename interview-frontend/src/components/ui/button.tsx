import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";

import { cn } from "@/lib/utils";

const buttonVariants = cva(
  "inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-xl text-sm font-medium transition-all duration-300 disabled:pointer-events-none disabled:opacity-50 outline-none focus-visible:ring-2 focus-visible:ring-sky-400/70",
  {
    variants: {
      variant: {
        default:
          "bg-sky-500/90 text-white shadow-[0_8px_35px_rgba(14,165,233,0.35)] hover:scale-[1.02] hover:bg-sky-400 hover:shadow-[0_10px_40px_rgba(56,189,248,0.5)]",
        secondary:
          "bg-slate-800/80 text-slate-100 border border-slate-600/40 hover:bg-slate-700/80 hover:scale-[1.01]",
        ghost: "bg-transparent text-slate-300 hover:bg-slate-800/70 hover:text-white",
      },
      size: {
        default: "h-11 px-5 py-2",
        sm: "h-9 rounded-lg px-3",
        lg: "h-12 px-7 text-base",
        icon: "size-10",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  },
);

function Button({
  className,
  variant,
  size,
  asChild = false,
  ...props
}: React.ComponentProps<"button"> &
  VariantProps<typeof buttonVariants> & {
    asChild?: boolean;
  }) {
  const Comp = asChild ? ("span" as const) : "button";

  return (
    <Comp
      data-slot="button"
      className={cn(buttonVariants({ variant, size, className }))}
      {...props}
    />
  );
}

export { Button, buttonVariants };
