import { useState } from "react";
import { BarChart3, Users } from "lucide-react";
import { BooksView } from "./pages/BooksView";
import { LoggedOutLanding } from "./pages/LoggedOutLanding";
import { ResultsView } from "./pages/ResultsView";
import { StudentPoll } from "./pages/StudentPoll";
import { TeacherLanding } from "./pages/TeacherLanding";
import type { AssignmentResults, ClassPeriod } from "./types";
import type { AuthMode, View } from "./utils/appTypes";

const storedToken = localStorage.getItem("bookranker.teacherToken") ?? "";
const storedClassId = localStorage.getItem("bookranker.classId") ?? "";

function App() {
  const [view, setView] = useState<View>("classes");
  const [authMode, setAuthMode] = useState<AuthMode>("signup");
  const [token, setToken] = useState(storedToken);
  const [classId, setClassId] = useState(storedClassId);
  const [classPeriod, setClassPeriod] = useState<ClassPeriod | null>(null);
  const [latestAssignment, setLatestAssignment] = useState<AssignmentResults | null>(null);
  const isPollPath = window.location.pathname === "/poll" || window.location.pathname.startsWith("/poll/");

  function persistToken(nextToken: string) {
    setToken(nextToken);
    localStorage.setItem("bookranker.teacherToken", nextToken);
  }

  function signOut() {
    setToken("");
    setView("classes");
    setAuthMode("login");
    localStorage.removeItem("bookranker.teacherToken");
  }

  function persistClassId(nextClassId: string) {
    setClassId(nextClassId);
    localStorage.setItem("bookranker.classId", nextClassId);
  }

  function openClassBooks(details: ClassPeriod) {
    persistClassId(details.id);
    setClassPeriod(details);
    setView("books");
  }

  if (isPollPath) {
    return <StudentPoll />;
  }

  if (!token) {
    return <LoggedOutLanding mode={authMode} onMode={setAuthMode} onToken={persistToken} />;
  }

  return (
    <main className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">BookRanker</p>
          <h1>Class assignment workspace</h1>
        </div>
        <nav className="tabs" aria-label="Primary">
          <button className={view === "classes" || view === "books" ? "active" : ""} onClick={() => setView("classes")}>
            <Users size={17} /> Classes
          </button>
          <button className={view === "results" ? "active" : ""} onClick={() => setView("results")}>
            <BarChart3 size={17} /> Results
          </button>
        </nav>
        <button className="topbar-link" onClick={signOut}>
          Sign out
        </button>
      </header>

      {view === "classes" && <TeacherLanding token={token} onOpenClass={openClassBooks} />}
      {view === "books" && (
        <BooksView
          token={token}
          classId={classId}
          classPeriod={classPeriod}
          onClassPeriod={setClassPeriod}
          onAssignment={setLatestAssignment}
          onViewResults={() => setView("results")}
        />
      )}
      {view === "results" && (
        <ResultsView
          token={token}
          classId={classId}
          classPeriod={classPeriod}
          latestAssignment={latestAssignment}
          onClassPeriod={setClassPeriod}
          onAssignment={setLatestAssignment}
        />
      )}
    </main>
  );
}

export default App;
