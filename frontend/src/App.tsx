import { useEffect, useMemo, useState } from "react";
import {
  BarChart3,
  BookOpen,
  Check,
  ClipboardList,
  Copy,
  Edit3,
  Loader2,
  LogIn,
  Plus,
  RefreshCw,
  Send,
  Trash2,
  UserPlus,
  Users
} from "lucide-react";
import { api } from "./api/client";
import type {
  AssignmentResults,
  AssignmentRun,
  Book,
  ClassAssignmentGrid,
  ClassPeriod,
  RankingItem,
  StudentStatus,
  TeacherAssignmentGrid
} from "./types";

type View = "classes" | "books" | "results";
type AuthMode = "signup" | "login";
type Notice = { kind: "success" | "error"; message: string } | null;
type Confirmation = {
  title: string;
  message: string;
  confirmLabel?: string;
  onConfirm: () => void;
} | null;
type TeacherClassSummary = {
  id: string;
  name: string;
  joinCode: string;
};

const storedToken = localStorage.getItem("bookranker.teacherToken") ?? "";
const storedClassId = localStorage.getItem("bookranker.classId") ?? "";

function getPollJoinCode() {
  const path = window.location.pathname;

  if (!path.startsWith("/poll/")) {
    return "";
  }

  return decodeURIComponent(path.slice("/poll/".length).split("/")[0] ?? "").trim().toUpperCase();
}

function buildPollUrl(joinCode: string | undefined) {
  if (!joinCode) {
    return "-";
  }

  return `${window.location.origin}/poll/${encodeURIComponent(joinCode)}`;
}

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

      {view === "classes" && (
        <TeacherLanding
          token={token}
          onOpenClass={openClassBooks}
        />
      )}
      {view === "books" && (
        <BooksView
          token={token}
          classId={classId}
          classPeriod={classPeriod}
          onClassId={persistClassId}
          onClassPeriod={setClassPeriod}
          onAssignment={setLatestAssignment}
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

function LoggedOutLanding({
  mode,
  onMode,
  onToken
}: {
  mode: AuthMode;
  onMode: (mode: AuthMode) => void;
  onToken: (token: string) => void;
}) {
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
          <p>
            Build a class reading list, collect student rankings, and run assignments from one teacher workspace.
          </p>
        </div>

        <article className="auth-panel">
          <div className="panel-heading">
            {isSignup ? <UserPlus size={18} /> : <LogIn size={18} />}
            <h2>{isSignup ? "Create teacher account" : "Teacher login"}</h2>
          </div>
          <div className="form-grid">
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

      <NoticeBanner notice={notice} />
    </main>
  );
}

type TeacherLandingProps = {
  token: string;
  onOpenClass: (classPeriod: ClassPeriod) => void;
};

function TeacherLanding(props: TeacherLandingProps) {
  const [classes, setClasses] = useState<TeacherClassSummary[]>([]);
  const [className, setClassName] = useState("");
  const [editingClassId, setEditingClassId] = useState("");
  const [editingClassName, setEditingClassName] = useState("");
  const [teacherGrid, setTeacherGrid] = useState<TeacherAssignmentGrid | null>(null);
  const [notice, setNotice] = useState<Notice>(null);
  const [loading, setLoading] = useState("");
  const [confirmation, setConfirmation] = useState<Confirmation>(null);

  useEffect(() => {
    void refreshClasses();
  }, [props.token]);

  async function refreshClasses() {
    return withNotice(setLoading, setNotice, "classes", async () => {
      const response = await api.listClassPeriods(props.token);
      setClasses(response.classes);
    });
  }

  async function createClassPeriod() {
    return withNotice(setLoading, setNotice, "class", async () => {
      const response = await api.createClassPeriod(props.token, className);
      const details = await api.getClassPeriod(props.token, response.classId);
      try {
        const nextClasses = await api.listClassPeriods(props.token);
        setClasses(nextClasses.classes);
      } catch {
        setClasses((currentClasses) => [
          { id: details.id, name: details.name, joinCode: details.joinCode },
          ...currentClasses.filter((classItem) => classItem.id !== details.id)
        ]);
      }
      setClassName("");
      props.onOpenClass(details);
      setNotice({ kind: "success", message: `Class created with join code ${response.joinCode}.` });
    });
  }

  async function openClass(classItem: TeacherClassSummary) {
    return withNotice(setLoading, setNotice, `open-${classItem.id}`, async () => {
      const details = await api.getClassPeriod(props.token, classItem.id);
      props.onOpenClass(details);
    });
  }

  async function updateClass(classItem: TeacherClassSummary) {
    return withNotice(setLoading, setNotice, `update-${classItem.id}`, async () => {
      await api.updateClassPeriod(props.token, classItem.id, editingClassName);
      const response = await api.listClassPeriods(props.token);
      setClasses(response.classes);
      setEditingClassId("");
      setEditingClassName("");
      setNotice({ kind: "success", message: "Class updated." });
    });
  }

  async function deleteClass(classItem: TeacherClassSummary) {
    return withNotice(setLoading, setNotice, `delete-${classItem.id}`, async () => {
      await api.deleteClassPeriod(props.token, classItem.id);
      const response = await api.listClassPeriods(props.token);
      setClasses(response.classes);
      setNotice({ kind: "success", message: "Class deleted." });
    });
  }

  function confirmDeleteClass(classItem: TeacherClassSummary) {
    setConfirmation({
      title: "Remove class",
      message: `Remove ${classItem.name}? This will delete the class and its related books, students, rankings, and assignments.`,
      confirmLabel: "Remove",
      onConfirm: () => {
        setConfirmation(null);
        void deleteClass(classItem);
      }
    });
  }

  async function loadTeacherAssignmentGrid() {
    return withNotice(setLoading, setNotice, "teacher-grid", async () => {
      const grid = await api.getTeacherAssignmentGrid(props.token);
      setTeacherGrid(grid);
      setNotice({ kind: "success", message: "Assignment spreadsheet loaded." });
    });
  }

  return (
    <section className="teacher-home">
      <Panel title="Create class" icon={<Plus size={18} />}>
        <div className="form-grid">
          <label>
            Class name
            <input value={className} onChange={(event) => setClassName(event.target.value)} />
          </label>
        </div>
        <ActionButton icon={<Plus size={16} />} label="Create class" busy={loading === "class"} onClick={createClassPeriod} />
      </Panel>

      <Panel title="Current classes" icon={<Users size={18} />}>
        <div className="list-toolbar">
          <ActionButton
            icon={<ClipboardList size={16} />}
            label="Assignment spreadsheet"
            busy={loading === "teacher-grid"}
            onClick={loadTeacherAssignmentGrid}
            variant="secondary"
          />
          <ActionButton
            icon={<RefreshCw size={16} />}
            label="Refresh"
            busy={loading === "classes"}
            onClick={refreshClasses}
            variant="secondary"
          />
        </div>
        <div className="class-list">
          {classes.length === 0 && <p className="empty-state">No classes yet.</p>}
          {classes.map((classItem) => (
            <div className="class-row" key={classItem.id}>
              {editingClassId === classItem.id ? (
                <label className="inline-edit">
                  Class name
                  <input value={editingClassName} onChange={(event) => setEditingClassName(event.target.value)} />
                </label>
              ) : (
                <button className="class-open" onClick={() => openClass(classItem)}>
                  <span>
                    <strong>{classItem.name}</strong>
                    <small>{classItem.joinCode}</small>
                  </span>
                  <BookOpen size={18} />
                </button>
              )}
              <div className="table-actions">
                {editingClassId === classItem.id ? (
                  <>
                    <button onClick={() => updateClass(classItem)}>
                      <Check size={15} /> Save
                    </button>
                    <button
                      onClick={() => {
                        setEditingClassId("");
                        setEditingClassName("");
                      }}
                    >
                      Cancel
                    </button>
                  </>
                ) : (
                  <>
                    <button
                      onClick={() => {
                        setEditingClassId(classItem.id);
                        setEditingClassName(classItem.name);
                      }}
                    >
                      <Edit3 size={15} /> Edit
                    </button>
                    <button onClick={() => confirmDeleteClass(classItem)}>
                      <Trash2 size={15} /> Remove
                    </button>
                  </>
                )}
              </div>
            </div>
          ))}
        </div>
      </Panel>

      {teacherGrid && (
        <Panel title="All class assignments" icon={<ClipboardList size={18} />} wide>
          <TeacherAssignmentSpreadsheet grid={teacherGrid} />
        </Panel>
      )}

      <NoticeBanner notice={notice} />
      <ConfirmationDialog confirmation={confirmation} onCancel={() => setConfirmation(null)} />
    </section>
  );
}

type BooksViewProps = {
  token: string;
  classId: string;
  classPeriod: ClassPeriod | null;
  onClassId: (classId: string) => void;
  onClassPeriod: (classPeriod: ClassPeriod) => void;
  onAssignment: (assignment: AssignmentResults) => void;
};

function BooksView(props: BooksViewProps) {
  const [bookTitle, setBookTitle] = useState("");
  const [capacity, setCapacity] = useState(1);
  const [notice, setNotice] = useState<Notice>(null);
  const [loading, setLoading] = useState("");
  const [copiedStudentUrl, setCopiedStudentUrl] = useState(false);
  const [confirmation, setConfirmation] = useState<Confirmation>(null);
  const studentUrl = buildPollUrl(props.classPeriod?.joinCode);
  const totalCapacity = (props.classPeriod?.books ?? []).reduce((sum, book) => sum + book.capacity, 0);
  const studentCount = props.classPeriod?.students.length ?? 0;
  const hasTooManyStudents = studentCount > totalCapacity;

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

  async function refreshClassPeriod() {
    return withNotice(setLoading, setNotice, "refresh-books", async () => {
      const details = await api.getClassPeriod(props.token, props.classId);
      props.onClassPeriod(details);
      setNotice({ kind: "success", message: "Books loaded." });
    });
  }

  async function updateBook(book: Book, title: string, nextCapacity: number) {
    return withNotice(setLoading, setNotice, `update-book-${book.id}`, async () => {
      await api.updateBook(props.token, props.classId, book.id, title, nextCapacity);
      const details = await api.getClassPeriod(props.token, props.classId);
      props.onClassPeriod(details);
      setNotice({ kind: "success", message: "Book updated." });
    });
  }

  async function deleteBook(book: Book) {
    return withNotice(setLoading, setNotice, `delete-book-${book.id}`, async () => {
      await api.deleteBook(props.token, props.classId, book.id);
      const details = await api.getClassPeriod(props.token, props.classId);
      props.onClassPeriod(details);
      setNotice({ kind: "success", message: "Book removed." });
    });
  }

  function confirmDeleteBook(book: Book) {
    setConfirmation({
      title: "Remove book",
      message: `Remove ${book.title}? Existing student rankings and assignments for this book may be affected.`,
      confirmLabel: "Remove",
      onConfirm: () => {
        setConfirmation(null);
        void deleteBook(book);
      }
    });
  }

  async function updateStudent(student: ClassPeriod["students"][number], username: string) {
    return withNotice(setLoading, setNotice, `update-student-${student.id}`, async () => {
      await api.updateStudent(props.token, props.classId, student.id, username);
      const details = await api.getClassPeriod(props.token, props.classId);
      props.onClassPeriod(details);
      setNotice({ kind: "success", message: "Student updated." });
    });
  }

  async function deleteStudent(student: ClassPeriod["students"][number]) {
    return withNotice(setLoading, setNotice, `delete-student-${student.id}`, async () => {
      await api.deleteStudent(props.token, props.classId, student.id);
      const details = await api.getClassPeriod(props.token, props.classId);
      props.onClassPeriod(details);
      setNotice({ kind: "success", message: "Student removed." });
    });
  }

  async function clearClassStudents() {
    return withNotice(setLoading, setNotice, "clear-students", async () => {
      await api.clearClassStudents(props.token, props.classId);
      const details = await api.getClassPeriod(props.token, props.classId);
      props.onClassPeriod(details);
      setNotice({ kind: "success", message: "Student data cleared." });
    });
  }

  function confirmDeleteStudent(student: ClassPeriod["students"][number]) {
    setConfirmation({
      title: "Remove student",
      message: `Remove ${student.username}? Their rankings and assignment records may be deleted.`,
      confirmLabel: "Remove",
      onConfirm: () => {
        setConfirmation(null);
        void deleteStudent(student);
      }
    });
  }

  function confirmClearClassStudents() {
    setConfirmation({
      title: "Clear student data",
      message: `Clear all student data from ${props.classPeriod?.name ?? "this class"}? This removes students, rankings, assignment results, and assignment runs while preserving the class, join code, and books.`,
      confirmLabel: "Clear",
      onConfirm: () => {
        setConfirmation(null);
        void clearClassStudents();
      }
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

  async function copyStudentUrl() {
    if (studentUrl === "-") {
      setNotice({ kind: "error", message: "No student URL is available yet." });
      return;
    }

    try {
      await navigator.clipboard.writeText(studentUrl);
      setCopiedStudentUrl(true);
      window.setTimeout(() => setCopiedStudentUrl(false), 1400);
      setNotice({ kind: "success", message: "Student URL copied." });
    } catch {
      setNotice({ kind: "error", message: "Copy failed. Select the URL and copy it manually." });
    }
  }

  return (
    <section className="workspace-grid">
      <Panel title={props.classPeriod?.name ?? "Books"} icon={<BookOpen size={18} />} wide>
        <div className="detail-header">
          <ActionButton
            icon={<RefreshCw size={16} />}
            label="Refresh"
            busy={loading === "refresh-books"}
            onClick={refreshClassPeriod}
            variant="secondary"
          />
        </div>
      </Panel>

      <Panel title="Assignment" icon={<BarChart3 size={18} />}>
        <div className="metric-grid">
          <Metric label="Books" value={props.classPeriod?.books.length ?? 0} />
          <Metric label="Capacity" value={totalCapacity} />
          <Metric label="Students" value={studentCount} valueTone={hasTooManyStudents ? "danger" : undefined} />
        </div>
        <ActionButton icon={<Check size={16} />} label="Run assignment" busy={loading === "assign"} onClick={runAssignment} />
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
        <EditableBookTable
          books={props.classPeriod?.books ?? []}
          onUpdate={updateBook}
          onDelete={confirmDeleteBook}
        />
      </Panel>

      <Panel title="Students" icon={<ClipboardList size={18} />} wide>
        <div className="metric-grid two">
          <CopyableMetric label="Student URL" value={studentUrl} copied={copiedStudentUrl} onCopy={copyStudentUrl} />
          <Metric label="Students" value={studentCount} valueTone={hasTooManyStudents ? "danger" : undefined} />
        </div>
        <div className="section-actions">
          <ActionButton
            icon={<Users size={16} />}
            label="Clear students"
            busy={loading === "clear-students"}
            onClick={confirmClearClassStudents}
            variant="secondary"
          />
        </div>
        <EditableStudentTable
          students={props.classPeriod?.students ?? []}
          onUpdate={updateStudent}
          onDelete={confirmDeleteStudent}
        />
      </Panel>

      <NoticeBanner notice={notice} />
      <ConfirmationDialog confirmation={confirmation} onCancel={() => setConfirmation(null)} />
    </section>
  );
}

function StudentPoll() {
  const urlJoinCode = getPollJoinCode();
  const hasUrlJoinCode = Boolean(urlJoinCode);
  const [joinCode, setJoinCode] = useState(urlJoinCode);
  const [publicAssignmentGrid, setPublicAssignmentGrid] = useState<ClassAssignmentGrid | null>(null);
  const [assignmentGridChecked, setAssignmentGridChecked] = useState(!hasUrlJoinCode);
  const [username, setUsername] = useState("");
  const [studentId, setStudentId] = useState("");
  const [classId, setClassId] = useState("");
  const [classDisplayName, setClassDisplayName] = useState("");
  const [books, setBooks] = useState<Book[]>([]);
  const [notice, setNotice] = useState<Notice>(null);
  const [loading, setLoading] = useState("");
  const [status, setStatus] = useState<StudentStatus | null>(null);
  const [rankings, setRankings] = useState<Record<string, number>>({});
  const [existingMember, setExistingMember] = useState(false);
  const hasPreviousRankings = Boolean(status && status.rankCount > 0);

  useEffect(() => {
    let active = true;

    if (!urlJoinCode) {
      setAssignmentGridChecked(true);
      return;
    }

    setAssignmentGridChecked(false);
    setPublicAssignmentGrid(null);

    api.getClassAssignmentGrid(urlJoinCode)
      .then((grid) => {
        if (active) {
          setPublicAssignmentGrid(grid.assignmentRunId ? grid : null);
          setAssignmentGridChecked(true);
        }
      })
      .catch(() => {
        if (active) {
          setPublicAssignmentGrid(null);
          setAssignmentGridChecked(true);
        }
      });

    return () => {
      active = false;
    };
  }, [urlJoinCode]);

  async function joinClassPeriod() {
    return withNotice(setLoading, setNotice, "join", async () => {
      const response = await api.joinClassPeriod(joinCode, username);
      const [nextStatus, bookResponse] = await Promise.all([
        api.getStudentStatus(response.studentId),
        api.getStudentBooks(response.studentId)
      ]);
      setStudentId(response.studentId);
      setClassId(response.classId);
      setClassDisplayName(response.className ?? bookResponse.className ?? (joinCode || response.classId));
      setStatus(nextStatus);
      setBooks(bookResponse.books);
      setRankings({});
      setExistingMember(response.existingMember);
      setNotice({
        kind: "success",
        message: response.existingMember ? "Existing class membership found." : "Joined class."
      });
    });
  }

  async function submitRankings() {
    return withNotice(setLoading, setNotice, "rankings", async () => {
      const payload: RankingItem[] = Object.entries(rankings)
        .filter(([, rank]) => rank > 0)
        .map(([bookId, rank]) => ({ bookId, rank }));
      await api.submitRankings(studentId, payload);
      const nextStatus = await api.getStudentStatus(studentId);
      setStatus(nextStatus);
      setNotice({ kind: "success", message: "Rankings submitted." });
    });
  }

  return (
    <main className="public-shell">
      <header className="public-topbar">
        <div className="brand-mark">
          <BookOpen size={22} />
          <span>BookRanker</span>
        </div>
      </header>

      <section className="poll-grid">
        {!assignmentGridChecked && (
          <Panel title="Loading assignment" icon={<Loader2 className="spin" size={18} />} wide>
            <p className="empty-state">Checking whether assignments are available.</p>
          </Panel>
        )}

        {publicAssignmentGrid && (
          <Panel title="Assignment results" icon={<ClipboardList size={18} />} wide>
            <ClassAssignmentSpreadsheet grid={publicAssignmentGrid} />
          </Panel>
        )}

        {assignmentGridChecked && !publicAssignmentGrid && !studentId && (
          <Panel title="Join class" icon={<UserPlus size={18} />}>
            <div className={`form-grid ${hasUrlJoinCode ? "" : "two"}`}>
              {!hasUrlJoinCode && (
                <label>
                  Join code
                  <input value={joinCode} onChange={(event) => setJoinCode(event.target.value.toUpperCase())} />
                </label>
              )}
              <label>
                Username
                <input value={username} onChange={(event) => setUsername(event.target.value)} />
              </label>
            </div>
            <ActionButton icon={<Send size={16} />} label="Continue" busy={loading === "join"} onClick={joinClassPeriod} />
          </Panel>
        )}

        {assignmentGridChecked && !publicAssignmentGrid && studentId && (
          <Panel title="Rank books" icon={<BookOpen size={18} />} wide>
            <div className="poll-heading">
              <div>
                <p className="eyebrow">Student</p>
                <strong>{username}</strong>
              </div>
              <div>
                <p className="eyebrow">Class</p>
                <strong>{classDisplayName}</strong>
              </div>
            </div>
            {existingMember && (
              <div className="inline-notice">
                This student is already a member of this class. You can revise the rankings below; submitting again will replace
                the old rankings.
              </div>
            )}
            <div className="ranking-list">
              {books.map((book) => (
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
            <ActionButton
              icon={<Check size={16} />}
              label={existingMember || hasPreviousRankings ? "Submit revised rankings" : "Submit rankings"}
              busy={loading === "rankings"}
              onClick={submitRankings}
            />
          </Panel>
        )}

        {assignmentGridChecked && !publicAssignmentGrid && status && (
          <Panel title="Progress" icon={<BarChart3 size={18} />}>
            <div className="metric-grid two">
              <Metric label="Submitted" value={status.submitted ? "Yes" : "No"} />
              <Metric label="Ranked" value={`${status.rankCount}/${status.totalBooks}`} />
            </div>
          </Panel>
        )}

        <NoticeBanner notice={notice} />
      </section>
    </main>
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
  const unassignedStudents = useMemo(() => {
    if (!props.latestAssignment) {
      return [];
    }

    const assignedStudentIds = new Set(props.latestAssignment.results.map((result) => result.studentId));
    return (props.classPeriod?.students ?? []).filter((student) => !assignedStudentIds.has(student.id));
  }, [props.classPeriod, props.latestAssignment]);

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
        <div className="metric-grid five">
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
            {unassignedStudents.map((student) => (
              <tr key={`${student.id}-unassigned`}>
                <td>{student.username}</td>
                <td className="danger-text">Unassigned</td>
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

function Metric({
  label,
  value,
  valueTone
}: {
  label: string;
  value: string | number;
  valueTone?: "danger";
}) {
  return (
    <div className="metric">
      <span>{label}</span>
      <strong className={valueTone === "danger" ? "danger-text" : undefined}>{value}</strong>
    </div>
  );
}

function CopyableMetric({
  label,
  value,
  copied,
  onCopy
}: {
  label: string;
  value: string | number;
  copied: boolean;
  onCopy: () => void;
}) {
  return (
    <div className="metric copyable-metric">
      <button type="button" aria-label={`Copy ${label}`} onClick={onCopy}>
        {copied ? <Check size={15} /> : <Copy size={15} />}
      </button>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function ClassAssignmentSpreadsheet({ grid }: { grid: ClassAssignmentGrid }) {
  return (
    <div className="spreadsheet-wrap public-assignment-grid">
      <div className="spreadsheet-meta">
        <Metric label="Class" value={grid.className} />
        <Metric label="Join code" value={grid.joinCode} />
        <Metric label="Run" value={grid.assignmentRunId ?? "-"} />
      </div>
      <table className="spreadsheet-table">
        <thead>
          <tr>
            <th>Book</th>
            <th>Students</th>
          </tr>
        </thead>
        <tbody>
          {grid.rows.map((row, rowIndex) => (
            <tr key={`${row.bookTitle}-${rowIndex}`}>
              <th scope="row">{row.bookTitle}</th>
              <td>
                {row.students.length > 0 ? (
                  <div className="student-chip-list">
                    {row.students.map((student) => (
                      <span key={student}>{student}</span>
                    ))}
                  </div>
                ) : (
                  <span className="empty-cell">No assignment</span>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function TeacherAssignmentSpreadsheet({ grid }: { grid: TeacherAssignmentGrid }) {
  const columns = grid.columns
    .map((column, index) => ({
      key: column.key ?? column.classId ?? `column-${index}`,
      label: column.label ?? column.className ?? column.key ?? column.classId ?? `Column ${index + 1}`
    }))
    .filter((column) => column.key !== "book");

  return (
    <div className="spreadsheet-wrap teacher-assignment-grid">
      <table className="spreadsheet-table teacher-spreadsheet">
        <thead>
          <tr>
            <th>Book</th>
            {columns.map((column) => (
              <th key={column.key}>{column.label}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {grid.rows.map((row, rowIndex) => (
            <tr key={`${row.bookTitle}-${rowIndex}`}>
              <th scope="row">{row.bookTitle}</th>
              {columns.map((column) => (
                <td key={column.key}>
                  {row.cells[column.key] ? <span className="student-cell">{row.cells[column.key]}</span> : ""}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function EditableBookTable({
  books,
  onUpdate,
  onDelete
}: {
  books: Book[];
  onUpdate: (book: Book, title: string, capacity: number) => void;
  onDelete: (book: Book) => void;
}) {
  const [editingBookId, setEditingBookId] = useState("");
  const [title, setTitle] = useState("");
  const [capacity, setCapacity] = useState(1);

  return (
    <table>
      <thead>
        <tr>
          <th>Title</th>
          <th>Capacity</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {books.map((book) => (
          <tr key={book.id}>
            <td>
              {editingBookId === book.id ? (
                <input value={title} onChange={(event) => setTitle(event.target.value)} aria-label={`Title for ${book.title}`} />
              ) : (
                book.title
              )}
            </td>
            <td>
              {editingBookId === book.id ? (
                <input
                  value={capacity}
                  min={1}
                  onChange={(event) => setCapacity(Number(event.target.value))}
                  type="number"
                  aria-label={`Capacity for ${book.title}`}
                />
              ) : (
                book.capacity
              )}
            </td>
            <td>
              <div className="table-actions">
                {editingBookId === book.id ? (
                  <>
                    <button
                      onClick={() => {
                        onUpdate(book, title, capacity);
                        setEditingBookId("");
                      }}
                    >
                      <Check size={15} /> Save
                    </button>
                    <button onClick={() => setEditingBookId("")}>Cancel</button>
                  </>
                ) : (
                  <>
                    <button
                      onClick={() => {
                        setEditingBookId(book.id);
                        setTitle(book.title);
                        setCapacity(book.capacity);
                      }}
                    >
                      <Edit3 size={15} /> Edit
                    </button>
                    <button onClick={() => onDelete(book)}>
                      <Trash2 size={15} /> Remove
                    </button>
                  </>
                )}
              </div>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function EditableStudentTable({
  students,
  onUpdate,
  onDelete
}: {
  students: ClassPeriod["students"];
  onUpdate: (student: ClassPeriod["students"][number], username: string) => void;
  onDelete: (student: ClassPeriod["students"][number]) => void;
}) {
  const [editingStudentId, setEditingStudentId] = useState("");
  const [username, setUsername] = useState("");

  return (
    <table>
      <thead>
        <tr>
          <th>Username</th>
          <th>Student ID</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {students.map((student) => (
          <tr key={student.id}>
            <td>
              {editingStudentId === student.id ? (
                <input
                  value={username}
                  onChange={(event) => setUsername(event.target.value)}
                  aria-label={`Username for ${student.username}`}
                />
              ) : (
                student.username
              )}
            </td>
            <td>{student.id}</td>
            <td>
              <div className="table-actions">
                {editingStudentId === student.id ? (
                  <>
                    <button
                      onClick={() => {
                        onUpdate(student, username);
                        setEditingStudentId("");
                      }}
                    >
                      <Check size={15} /> Save
                    </button>
                    <button onClick={() => setEditingStudentId("")}>Cancel</button>
                  </>
                ) : (
                  <>
                    <button
                      onClick={() => {
                        setEditingStudentId(student.id);
                        setUsername(student.username);
                      }}
                    >
                      <Edit3 size={15} /> Edit
                    </button>
                    <button onClick={() => onDelete(student)}>
                      <Trash2 size={15} /> Remove
                    </button>
                  </>
                )}
              </div>
            </td>
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

function ConfirmationDialog({
  confirmation,
  onCancel
}: {
  confirmation: Confirmation;
  onCancel: () => void;
}) {
  if (!confirmation) {
    return null;
  }

  return (
    <div className="modal-backdrop" role="presentation">
      <div className="confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="confirm-dialog-title">
        <div>
          <h2 id="confirm-dialog-title">{confirmation.title}</h2>
          <p>{confirmation.message}</p>
        </div>
        <div className="confirm-actions">
          <button type="button" onClick={onCancel}>
            Cancel
          </button>
          <button type="button" className="danger" onClick={confirmation.onConfirm}>
            {confirmation.confirmLabel ?? "Confirm"}
          </button>
        </div>
      </div>
    </div>
  );
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
