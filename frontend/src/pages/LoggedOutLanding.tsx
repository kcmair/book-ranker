import { BookOpen, LogIn, UserPlus } from "lucide-react";
import { useState } from "react";
import { api } from "../api/client";
import { ActionButton, NoticeModal } from "../components/ui";
import type { AuthMode, Notice } from "../utils/appTypes";
import { withNotice } from "../utils/notices";
import { submitOnEnter } from "../utils/viewHelpers";

type LoggedOutLandingProps = {
  mode: AuthMode;
  onMode: (mode: AuthMode) => void;
  onToken: (token: string) => void;
};

export function LoggedOutLanding({ mode, onMode, onToken }: LoggedOutLandingProps) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [notice, setNotice] = useState<Notice>(null);
  const [loading, setLoading] = useState("");
  const isSignup = mode === "signup";

  async function submitAuth() {
    return withNotice(setLoading, setNotice, mode, async () => {
      if (isSignup) {
        await api.registerTeacher(email, password);
      }

      const response = await api.loginTeacher(email, password);
      onToken(response.token);
      setNotice({ kind: "success", message: isSignup ? "Account created." : "Signed in." });
    });
  }

  return (
    <main className="public-shell">
      <header className="public-topbar">
        <div className="brand-mark">
          <BookOpen size={22} />
          <span>BookRanker</span>
        </div>
        <button className="topbar-link" onClick={() => onMode(isSignup ? "login" : "signup")}>
          {isSignup ? "Log in" : "Create account"}
        </button>
      </header>

      <section className="landing-grid">
        <div className="landing-copy">
          <p className="eyebrow">Class book assignments</p>
          <h1>Rank books, balance capacity, assign fairly.</h1>
          <p>Build a class reading list, collect student rankings, and run assignments from one teacher workspace.</p>
        </div>

        <article className="auth-panel">
          <div className="panel-heading">
            {isSignup ? <UserPlus size={18} /> : <LogIn size={18} />}
            <h2>{isSignup ? "Create teacher account" : "Teacher login"}</h2>
          </div>
          <div className="form-grid">
            <label>
              Email
              <input
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                onKeyDown={submitOnEnter(submitAuth)}
                type="email"
                autoComplete="email"
              />
            </label>
            <label>
              Password
              <input
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                onKeyDown={submitOnEnter(submitAuth)}
                type="password"
                autoComplete={isSignup ? "new-password" : "current-password"}
              />
            </label>
          </div>
          <ActionButton
            icon={isSignup ? <UserPlus size={16} /> : <LogIn size={16} />}
            label={isSignup ? "Sign up" : "Log in"}
            busy={loading === mode}
            onClick={submitAuth}
          />
        </article>
      </section>

      <NoticeModal notice={notice} onDismiss={() => setNotice(null)} />
    </main>
  );
}
