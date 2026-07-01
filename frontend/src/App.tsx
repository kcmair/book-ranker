import { useMemo, useState } from "react";
import {
  BarChart3,
  BookOpen,
  Check,
  ClipboardList,
  KeyRound,
  Loader2,
  LogIn,
  Plus,
  RefreshCw,
  Send,
  UserPlus,
  Users
} from "lucide-react";
import { api } from "./api/client";
import type { AssignmentResults, AssignmentRun, Book, ClassPeriod, RankingItem, StudentStatus } from "./types";

type View = "teacher" | "student" | "results";
type Notice = { kind: "success" | "error"; message: string } | null;

const storedToken = localStorage.getItem("bookranker.teacherToken") ?? "";
const storedClassId = localStorage.getItem("bookranker.classId") ?? "";
const storedStudentId = localStorage.getItem("bookranker.studentId") ?? "";

function App() {
  const [view, setView] = useState<View>("teacher");
  const [token, setToken] = useState(storedToken);
  const [classId, setClassId] = useState(storedClassId);
  const [studentId, setStudentId] = useState(storedStudentId);
  const [classPeriod, setClassPeriod] = useState<ClassPeriod | null>(null);
  const [latestAssignment, setLatestAssignment] = useState<AssignmentResults | null>(null);

  function persistToken(nextToken: string) {
    setToken(nextToken);
    localStorage.setItem("bookranker.teacherToken", nextToken);
  }

  function persistClassId(nextClassId: string) {
    setClassId(nextClassId);
    localStorage.setItem("bookranker.classId", nextClassId);
  }

  function persistStudentId(nextStudentId: string) {
    setStudentId(nextStudentId);
    localStorage.setItem("bookranker.studentId", nextStudentId);
  }

  return (
    <main className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">BookRanker</p>
          <h1>Class assignment workspace</h1>
        </div>
        <nav className="tabs" aria-label="Primary">
          <button className={view === "teacher" ? "active" : ""} onClick={() => setView("teacher")}>
            <Users size={17} /> Teacher
          </button>
          <button className={view === "student" ? "active" : ""} onClick={() => setView("student")}>
            <BookOpen size={17} /> Student
          </button>
          <button className={view === "results" ? "active" : ""} onClick={() => setView("results")}>
            <BarChart3 size={17} /> Results
          </button>
        </nav>
      </header>

      {view === "teacher" && (
        <TeacherView
          token={token}
          classId={classId}
          classPeriod={classPeriod}
          onToken={persistToken}
          onClassId={persistClassId}
          onClassPeriod={setClassPeriod}
          onAssignment={setLatestAssignment}
        />
      )}
      {view === "student" && (
        <StudentView
          studentId={studentId}
          classId={classId}
          token={token}
          classPeriod={classPeriod}
          onStudentId={persistStudentId}
          onClassId={persistClassId}
          onClassPeriod={setClassPeriod}
        />
      )}
      {view === "results" && (
        <ResultsView
          token={token}
          classId={classId}
          classPeriod={classPeriod}
          latestAssignment={latestAssignment}
          onClassId={persistClassId}
          onClassPeriod={setClassPeriod}
          onAssignment={setLatestAssignment}
        />
      )}
    </main>
  );
}

type TeacherViewProps = {
  token: string;
  classId: string;
  classPeriod: ClassPeriod | null;
  onToken: (token: string) => void;
  onClassId: (classId: string) => void;
  onClassPeriod: (classPeriod: ClassPeriod) => void;
  onAssignment: (assignment: AssignmentResults | null) => void;
};

function TeacherView(props: TeacherViewProps) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [className, setClassName] = useState("");
  const [bookTitle, setBookTitle] = useState("");
  const [capacity, setCapacity] = useState(1);
  const [notice, setNotice] = useState<Notice>(null);
  const [loading, setLoading] = useState("");

  async function submitAuth(mode: "register" | "login") {
    return withNotice(setLoading, setNotice, mode, async () => {
      if (mode === "register") {
        await api.registerTeacher(email, password);
      }
      const response = await api.loginTeacher(email, password);
      props.onToken(response.token);
      setNotice({ kind: "success", message: mode === "register" ? "Teacher registered and signed in." : "Signed in." });
    });
  }

  async function createClassPeriod() {
    return withNotice(setLoading, setNotice, "class", async () => {
      const response = await api.createClassPeriod(props.token, className);
      props.onClassId(response.classId);
      const details = await api.getClassPeriod(props.token, response.classId);
      props.onClassPeriod(details);
      setNotice({ kind: "success", message: `Class created with join code ${response.joinCode}.` });
    });
  }

  async function loadClassPeriod() {
    return withNotice(setLoading, setNotice, "load-class", async () => {
      const details = await api.getClassPeriod(props.token, props.classId);
      props.onClassPeriod(details);
      setNotice({ kind: "success", message: "Class loaded." });
    });
  }

  async function addBook() {
    return withNotice(setLoading, setNotice, "book", async () => {
      await api.addBook(props.token, props.classId, bookTitle, capacity);
      const details = await api.getClassPeriod(props.token, props.classId);
      props.onClassPeriod(details);
      setBookTitle("");
      setCapacity(1);
      setNotice({ kind: "success", message: "Book added." });
    });
  }

  async function runAssignment() {
    return withNotice(setLoading, setNotice, "assign", async () => {
      await api.runAssignment(props.token, props.classId);
      const latest = await api.getLatestAssignment(props.token, props.classId);
      props.onAssignment(latest);
      setNotice({ kind: "success", message: "Assignment run completed." });
    });
  }

  return (
    <section className="workspace-grid">
      <Panel title="Teacher access" icon={<KeyRound size={18} />}>
        <div className="form-grid two">
          <label>
            Email
            <input value={email} onChange={(event) => setEmail(event.target.value)} type="email" autoComplete="email" />
          </label>
          <label>
            Password
            <input
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              type="password"
              autoComplete="current-password"
            />
          </label>
        </div>
        <div className="button-row">
          <ActionButton icon={<LogIn size={16} />} label="Sign in" busy={loading === "login"} onClick={() => submitAuth("login")} />
          <ActionButton
            icon={<UserPlus size={16} />}
            label="Register"
            busy={loading === "register"}
            onClick={() => submitAuth("register")}
            variant="secondary"
          />
        </div>
      </Panel>

      <Panel title="Class period" icon={<Users size={18} />}>
        <div className="form-grid two">
          <label>
            Class name
            <input value={className} onChange={(event) => setClassName(event.target.value)} />
          </label>
          <label>
            Class ID
            <input value={props.classId} onChange={(event) => props.onClassId(event.target.value)} />
          </label>
        </div>
        <div className="button-row">
          <ActionButton icon={<Plus size={16} />} label="Create" busy={loading === "class"} onClick={createClassPeriod} />
          <ActionButton
            icon={<RefreshCw size={16} />}
            label="Load"
            busy={loading === "load-class"}
            onClick={loadClassPeriod}
            variant="secondary"
          />
        </div>
      </Panel>

      <Panel title="Book list" icon={<BookOpen size={18} />} wide>
        <div className="form-grid book-entry">
          <label>
            Title
            <input value={bookTitle} onChange={(event) => setBookTitle(event.target.value)} />
          </label>
          <label>
            Capacity
            <input
              value={capacity}
              min={1}
              onChange={(event) => setCapacity(Number(event.target.value))}
              type="number"
            />
          </label>
          <ActionButton icon={<Plus size={16} />} label="Add" busy={loading === "book"} onClick={addBook} />
        </div>
        <BookTable books={props.classPeriod?.books ?? []} />
      </Panel>

      <Panel title="Roster" icon={<ClipboardList size={18} />}>
        <Metric label="Join code" value={props.classPeriod?.joinCode ?? "-"} />
        <Metric label="Students" value={props.classPeriod?.students.length ?? 0} />
        <ul className="compact-list">
          {(props.classPeriod?.students ?? []).map((student) => (
            <li key={student.id}>{student.username}</li>
          ))}
        </ul>
      </Panel>

      <Panel title="Assignment" icon={<BarChart3 size={18} />}>
        <div className="metric-grid">
          <Metric label="Books" value={props.classPeriod?.books.length ?? 0} />
          <Metric label="Capacity" value={(props.classPeriod?.books ?? []).reduce((sum, book) => sum + book.capacity, 0)} />
          <Metric label="Students" value={props.classPeriod?.students.length ?? 0} />
        </div>
        <ActionButton icon={<Check size={16} />} label="Run assignment" busy={loading === "assign"} onClick={runAssignment} />
      </Panel>

      <NoticeBanner notice={notice} />
    </section>
  );
}

type StudentViewProps = {
  studentId: string;
  classId: string;
  token: string;
  classPeriod: ClassPeriod | null;
  onStudentId: (studentId: string) => void;
  onClassId: (classId: string) => void;
  onClassPeriod: (classPeriod: ClassPeriod) => void;
};

function StudentView(props: StudentViewProps) {
  const [joinCode, setJoinCode] = useState("");
  const [username, setUsername] = useState("");
  const [notice, setNotice] = useState<Notice>(null);
  const [loading, setLoading] = useState("");
  const [status, setStatus] = useState<StudentStatus | null>(null);
  const [rankings, setRankings] = useState<Record<string, number>>({});

  async function joinClassPeriod() {
    return withNotice(setLoading, setNotice, "join", async () => {
      const response = await api.joinClassPeriod(joinCode, username);
      props.onStudentId(response.studentId);
      props.onClassId(response.classId);
      setNotice({ kind: "success", message: "Joined class." });
    });
  }

  async function loadBooks() {
    return withNotice(setLoading, setNotice, "student-books", async () => {
      const details = await api.getClassPeriod(props.token, props.classId);
      props.onClassPeriod(details);
      setNotice({ kind: "success", message: "Books loaded." });
    });
  }

  async function submitRankings() {
    return withNotice(setLoading, setNotice, "rankings", async () => {
      const payload: RankingItem[] = Object.entries(rankings)
        .filter(([, rank]) => rank > 0)
        .map(([bookId, rank]) => ({ bookId, rank }));
      await api.submitRankings(props.studentId, payload);
      const nextStatus = await api.getStudentStatus(props.studentId);
      setStatus(nextStatus);
      setNotice({ kind: "success", message: "Rankings submitted." });
    });
  }

  async function refreshStatus() {
    return withNotice(setLoading, setNotice, "status", async () => {
      const nextStatus = await api.getStudentStatus(props.studentId);
      setStatus(nextStatus);
      setNotice({ kind: "success", message: "Status refreshed." });
    });
  }

  return (
    <section className="workspace-grid">
      <Panel title="Join class" icon={<UserPlus size={18} />}>
        <div className="form-grid two">
          <label>
            Join code
            <input value={joinCode} onChange={(event) => setJoinCode(event.target.value.toUpperCase())} />
          </label>
          <label>
            Username
            <input value={username} onChange={(event) => setUsername(event.target.value)} />
          </label>
        </div>
        <ActionButton icon={<Send size={16} />} label="Join" busy={loading === "join"} onClick={joinClassPeriod} />
      </Panel>

      <Panel title="Student session" icon={<KeyRound size={18} />}>
        <div className="form-grid two">
          <label>
            Student ID
            <input value={props.studentId} onChange={(event) => props.onStudentId(event.target.value)} />
          </label>
          <label>
            Class ID
            <input value={props.classId} onChange={(event) => props.onClassId(event.target.value)} />
          </label>
        </div>
        <div className="button-row">
          <ActionButton
            icon={<RefreshCw size={16} />}
            label="Load books"
            busy={loading === "student-books"}
            onClick={loadBooks}
          />
          <ActionButton
            icon={<ClipboardList size={16} />}
            label="Status"
            busy={loading === "status"}
            onClick={refreshStatus}
            variant="secondary"
          />
        </div>
      </Panel>

      <Panel title="Rankings" icon={<BookOpen size={18} />} wide>
        <div className="ranking-list">
          {(props.classPeriod?.books ?? []).map((book) => (
            <label className="ranking-row" key={book.id}>
              <span>
                <strong>{book.title}</strong>
                <small>Capacity {book.capacity}</small>
              </span>
              <input
                type="number"
                min={1}
                value={rankings[book.id] ?? ""}
                onChange={(event) => setRankings({ ...rankings, [book.id]: Number(event.target.value) })}
                aria-label={`Rank for ${book.title}`}
              />
            </label>
          ))}
        </div>
        <ActionButton icon={<Check size={16} />} label="Submit rankings" busy={loading === "rankings"} onClick={submitRankings} />
      </Panel>

      <Panel title="Progress" icon={<BarChart3 size={18} />}>
        <div className="metric-grid">
          <Metric label="Submitted" value={status?.submitted ? "Yes" : "No"} />
          <Metric label="Ranked" value={status ? `${status.rankCount}/${status.totalBooks}` : "0/0"} />
        </div>
      </Panel>

      <NoticeBanner notice={notice} />
    </section>
  );
}

type ResultsViewProps = {
  token: string;
  classId: string;
  classPeriod: ClassPeriod | null;
  latestAssignment: AssignmentResults | null;
  onClassId: (classId: string) => void;
  onClassPeriod: (classPeriod: ClassPeriod) => void;
  onAssignment: (assignment: AssignmentResults) => void;
};

function ResultsView(props: ResultsViewProps) {
  const [history, setHistory] = useState<AssignmentRun[]>([]);
  const [notice, setNotice] = useState<Notice>(null);
  const [loading, setLoading] = useState("");

  const bookNames = useMemo(
    () => new Map((props.classPeriod?.books ?? []).map((book) => [book.id, book.title])),
    [props.classPeriod]
  );
  const studentNames = useMemo(
    () => new Map((props.classPeriod?.students ?? []).map((student) => [student.id, student.username])),
    [props.classPeriod]
  );

  async function refreshResults() {
    return withNotice(setLoading, setNotice, "results", async () => {
      const [details, latest, runs] = await Promise.all([
        api.getClassPeriod(props.token, props.classId),
        api.getLatestAssignment(props.token, props.classId),
        api.getAssignmentHistory(props.token, props.classId)
      ]);
      props.onClassPeriod(details);
      props.onAssignment(latest);
      setHistory(runs.runs);
      setNotice({ kind: "success", message: "Results refreshed." });
    });
  }

  return (
    <section className="workspace-grid results-grid">
      <Panel title="Results lookup" icon={<RefreshCw size={18} />}>
        <label>
          Class ID
          <input value={props.classId} onChange={(event) => props.onClassId(event.target.value)} />
        </label>
        <ActionButton icon={<RefreshCw size={16} />} label="Refresh" busy={loading === "results"} onClick={refreshResults} />
      </Panel>

      <Panel title="Current metrics" icon={<BarChart3 size={18} />} wide>
        <div className="metric-grid six">
          <Metric label="Cost" value={props.latestAssignment?.totalCost ?? "-"} />
          <Metric label="Satisfaction" value={formatPercent(props.latestAssignment?.satisfactionScore)} />
          <Metric label="First choice" value={props.latestAssignment?.firstChoiceCount ?? 0} />
          <Metric label="Top three" value={props.latestAssignment?.topThreeCount ?? 0} />
          <Metric label="Below top three" value={props.latestAssignment?.worseThanThirdCount ?? 0} />
          <Metric label="Unassigned" value={props.latestAssignment?.unassignedStudentCount ?? 0} />
        </div>
      </Panel>

      <Panel title="Assignments" icon={<ClipboardList size={18} />} wide>
        <table>
          <thead>
            <tr>
              <th>Student</th>
              <th>Book</th>
            </tr>
          </thead>
          <tbody>
            {(props.latestAssignment?.results ?? []).map((result) => (
              <tr key={`${result.studentId}-${result.bookId}`}>
                <td>{studentNames.get(result.studentId) ?? result.studentId}</td>
                <td>{bookNames.get(result.bookId) ?? result.bookId}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </Panel>

      <Panel title="Run history" icon={<BarChart3 size={18} />} wide>
        <table>
          <thead>
            <tr>
              <th>Run</th>
              <th>Status</th>
              <th>Cost</th>
              <th>Satisfaction</th>
              <th>Created</th>
            </tr>
          </thead>
          <tbody>
            {history.map((run) => (
              <tr key={run.runId}>
                <td>{run.runId}</td>
                <td>{run.status}</td>
                <td>{run.totalCost}</td>
                <td>{formatPercent(run.satisfactionScore)}</td>
                <td>{run.createdAt ? new Date(run.createdAt).toLocaleString() : "-"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </Panel>

      <NoticeBanner notice={notice} />
    </section>
  );
}

type PanelProps = {
  title: string;
  icon: React.ReactNode;
  children: React.ReactNode;
  wide?: boolean;
};

function Panel({ title, icon, children, wide }: PanelProps) {
  return (
    <article className={`panel ${wide ? "wide" : ""}`}>
      <div className="panel-heading">
        {icon}
        <h2>{title}</h2>
      </div>
      {children}
    </article>
  );
}

function ActionButton({
  icon,
  label,
  busy,
  onClick,
  variant = "primary"
}: {
  icon: React.ReactNode;
  label: string;
  busy: boolean;
  onClick: () => void;
  variant?: "primary" | "secondary";
}) {
  return (
    <button className={`action-button ${variant}`} disabled={busy} onClick={onClick}>
      {busy ? <Loader2 className="spin" size={16} /> : icon}
      {label}
    </button>
  );
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="metric">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function BookTable({ books }: { books: Book[] }) {
  return (
    <table>
      <thead>
        <tr>
          <th>Title</th>
          <th>Capacity</th>
        </tr>
      </thead>
      <tbody>
        {books.map((book) => (
          <tr key={book.id}>
            <td>{book.title}</td>
            <td>{book.capacity}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function NoticeBanner({ notice }: { notice: Notice }) {
  if (!notice) {
    return null;
  }

  return <div className={`notice ${notice.kind}`}>{notice.message}</div>;
}

async function withNotice(
  setLoading: (key: string) => void,
  setNotice: (notice: Notice) => void,
  loadingKey: string,
  task: () => Promise<void>
) {
  setLoading(loadingKey);
  setNotice(null);

  try {
    await task();
  } catch (error) {
    setNotice({ kind: "error", message: error instanceof Error ? error.message : "Request failed." });
  } finally {
    setLoading("");
  }
}

function formatPercent(value: number | undefined) {
  if (value === undefined) {
    return "-";
  }

  return `${Math.round(value * 100)}%`;
}

export default App;
