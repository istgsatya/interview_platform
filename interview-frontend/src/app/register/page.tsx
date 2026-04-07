import { AuthForm } from "@/components/auth/auth-form";
import { AuthShell } from "@/components/auth/auth-shell";

export default function RegisterPage() {
  return (
    <AuthShell
      title="Create your command center"
      description="Register to unlock your personalized interview cockpit, live AI analysis, and skill-gap intelligence."
      alternateHref="/login"
      alternateLabel="Sign in"
      alternateText="Already have an account?"
    >
      <AuthForm mode="register" />
    </AuthShell>
  );
}
