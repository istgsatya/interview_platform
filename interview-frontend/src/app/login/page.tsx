import { AuthForm } from "@/components/auth/auth-form";
import { AuthShell } from "@/components/auth/auth-shell";

export default function LoginPage() {
  return (
    <AuthShell
      title="Welcome back"
      description="Sign in to continue your AI-powered interview journey with real-time scoring and feedback."
      alternateHref="/register"
      alternateLabel="Create account"
      alternateText="Need access?"
    >
      <AuthForm mode="login" />
    </AuthShell>
  );
}
