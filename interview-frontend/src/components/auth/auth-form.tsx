"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { AnimatePresence, motion } from "framer-motion";
import { AlertCircle, CheckCircle2, LockKeyhole, Mail } from "lucide-react";

import { login, register } from "@/lib/api-client";
import { tokenStore } from "@/lib/token";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Spinner } from "@/components/ui/spinner";

type AuthMode = "login" | "register";

type AuthFormProps = {
  mode: AuthMode;
};

const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function AuthForm({ mode }: AuthFormProps) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const router = useRouter();

  const validationError = useMemo(() => {
    if (!email || !password) return "Email and password are required.";
    if (!emailRegex.test(email)) return "Please enter a valid email address.";
    if (password.length < 8) return "Password must be at least 8 characters.";
    return null;
  }, [email, password]);

  const isLogin = mode === "login";

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setSuccess(null);

    if (validationError) {
      setError(validationError);
      return;
    }

    setIsLoading(true);

    try {
      const token = isLogin
        ? await login({ email: email.trim(), password })
        : await register({ email: email.trim(), password });

      if (!token) {
        throw new Error("Backend did not return a token.");
      }

      tokenStore.setToken(token);
      setSuccess(isLogin ? "Access granted. Launching dashboard..." : "Account created. Initializing workspace...");

      await new Promise((resolve) => setTimeout(resolve, 620));
      router.push("/dashboard");
    } catch (err: unknown) {
      const fallback = isLogin ? "Invalid credentials. Please try again." : "Registration failed. Try a different email.";
      const message =
        typeof err === "object" && err !== null && "response" in err
          ? fallback
          : "Network error. Is the backend running on port 8080?";
      setError(message);
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <motion.form
      onSubmit={handleSubmit}
      className="space-y-5"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ delay: 0.08, duration: 0.35 }}
    >
      <motion.div
        className="space-y-2"
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.12 }}
      >
        <Label htmlFor={`${mode}-email`}>Email</Label>
        <div className="relative">
          <Mail className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
          <Input
            id={`${mode}-email`}
            type="email"
            autoComplete="email"
            placeholder="you@company.com"
            className="pl-9"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </div>
      </motion.div>

      <motion.div
        className="space-y-2"
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.17 }}
      >
        <Label htmlFor={`${mode}-password`}>Password</Label>
        <div className="relative">
          <LockKeyhole className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
          <Input
            id={`${mode}-password`}
            type="password"
            autoComplete={isLogin ? "current-password" : "new-password"}
            placeholder="••••••••"
            className="pl-9"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </div>
      </motion.div>

      <AnimatePresence mode="wait" initial={false}>
        {error ? (
          <motion.div
            key={`error-${error}`}
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -6 }}
            className="flex items-start gap-2 rounded-xl border border-rose-500/30 bg-rose-500/10 p-3 text-sm text-rose-200"
          >
            <AlertCircle className="mt-0.5 size-4 shrink-0" />
            <p>{error}</p>
          </motion.div>
        ) : success ? (
          <motion.div
            key={`success-${success}`}
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -6 }}
            className="flex items-start gap-2 rounded-xl border border-emerald-500/30 bg-emerald-500/10 p-3 text-sm text-emerald-200"
          >
            <CheckCircle2 className="mt-0.5 size-4 shrink-0" />
            <p>{success}</p>
          </motion.div>
        ) : null}
      </AnimatePresence>

      <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.22 }}>
        <Button type="submit" size="lg" className="w-full" disabled={isLoading}>
          {isLoading ? (
            <>
              <Spinner />
              Processing...
            </>
          ) : isLogin ? (
            "Sign In"
          ) : (
            "Create Account"
          )}
        </Button>
      </motion.div>
    </motion.form>
  );
}
