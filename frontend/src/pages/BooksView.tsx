import { BarChart3, BookOpen, Check, Loader2, Plus, RefreshCw, Users } from "lucide-react";
import { useEffect, useState } from "react";
import { api } from "../api/client";
import { EditableBookTable, EditableStudentTable } from "../components/editableTables";
import { ActionButton, ConfirmationDialog, CopyableMetric, Metric, NoticeModal, Panel } from "../components/ui";
import type { AssignmentResults, Book, ClassPeriod } from "../types";
import type { Confirmation, Notice } from "../utils/appTypes";
import { withNotice } from "../utils/notices";
import { buildPollUrl, submitOnEnter } from "../utils/viewHelpers";

type BooksViewProps = {
  token: string;
  classId: string;
  classPeriod: ClassPeriod | null;
  onClassPeriod: (classPeriod: ClassPeriod) => void;
  onAssignment: (assignment: AssignmentResults) => void;
  onViewResults: () => void;
};

export function BooksView(props: BooksViewProps) {
  const [bookTitle, setBookTitle] = useState("");
  const [capacity, setCapacity] = useState(1);
  const [minimumRankingCount, setMinimumRankingCount] = useState(0);
  const [notice, setNotice] = useState<Notice>(null);
  const [loading, setLoading] = useState("");
  const [copiedStudentUrl, setCopiedStudentUrl] = useState(false);
  const [confirmation, setConfirmation] = useState<Confirmation>(null);
  const studentUrl = buildPollUrl(props.classPeriod?.joinCode);
  const totalCapacity = (props.classPeriod?.books ?? []).reduce((sum, book) => sum + book.capacity, 0);
  const studentCount = props.classPeriod?.students.length ?? 0;
  const hasTooManyStudents = studentCount > totalCapacity;
  const bookCount = props.classPeriod?.books.length ?? 0;

  useEffect(() => {
    setMinimumRankingCount(props.classPeriod?.minimumRankingCount ?? bookCount);
  }, [bookCount, props.classPeriod?.minimumRankingCount]);

  async function addBook() {
    return withNotice(setLoading, setNotice, "book", async () => {
      await api.addBook(props.token, props.classId, bookTitle, capacity);
      const details = await api.getClassPeriod(props.token, props.classId);
      props.onClassPeriod(details);
      setBookTitle("");
      setCapacity(1);
    });
  }

  async function refreshClassPeriod() {
    return withNotice(setLoading, setNotice, "refresh-books", async () => {
      const details = await api.getClassPeriod(props.token, props.classId);
      props.onClassPeriod(details);
      setNotice({ kind: "success", message: "Books loaded." });
    });
  }

  async function updateMinimumRankingCount() {
    if (!props.classPeriod) {
      return;
    }
    if (bookCount === 0) {
      setNotice({ kind: "error", message: "Add at least one book before setting minimum rankings." });
      return;
    }

    return withNotice(setLoading, setNotice, "minimum-ranking-count", async () => {
      const boundedMinimum = Math.min(bookCount, Math.max(1, minimumRankingCount));
      await api.updateClassPeriod(props.token, props.classId, props.classPeriod?.name ?? "", boundedMinimum);
      const details = await api.getClassPeriod(props.token, props.classId);
      props.onClassPeriod(details);
      setNotice({ kind: "success", message: "Minimum rankings updated." });
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
      props.onViewResults();
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

      <Panel title="Assignment" icon={<BarChart3 size={18} />} wide>
        <div className="metric-grid four">
          <Metric label="Books" value={bookCount} />
          <Metric label="Capacity" value={totalCapacity} />
          <Metric label="Students" value={studentCount} valueTone={hasTooManyStudents ? "danger" : undefined} />
          <label className="metric metric-input">
            <span>Min rankings per student</span>
            <div className="metric-input-row">
              <input
                value={minimumRankingCount}
                min={bookCount > 0 ? 1 : 0}
                max={bookCount}
                onChange={(event) => {
                  const nextMinimum = Number(event.target.value);
                  setMinimumRankingCount(Math.min(bookCount, Math.max(1, nextMinimum)));
                }}
                onKeyDown={submitOnEnter(updateMinimumRankingCount)}
                type="number"
                disabled={bookCount === 0}
              />
              <button
                className="metric-save-button"
                disabled={loading === "minimum-ranking-count" || bookCount === 0}
                onClick={updateMinimumRankingCount}
                title="Save required rankings"
                type="button"
              >
                {loading === "minimum-ranking-count" ? <Loader2 className="spin" size={15} /> : <Check size={15} />}
                Save
              </button>
            </div>
          </label>
        </div>
        <div className="metric-grid one">
          <CopyableMetric label="Student URL" value={studentUrl} copied={copiedStudentUrl} onCopy={copyStudentUrl} />
        </div>
        <ActionButton
          icon={<Check size={16} />}
          label="Run assignment"
          busy={loading === "assign"}
          onClick={runAssignment}
        />
      </Panel>

      <Panel title="Book list" icon={<BookOpen size={18} />} wide>
        <div className="form-grid book-entry">
          <label>
            Title
            <input
              value={bookTitle}
              onChange={(event) => setBookTitle(event.target.value)}
              onKeyDown={submitOnEnter(addBook)}
            />
          </label>
          <label>
            Capacity
            <input
              value={capacity}
              min={1}
              onChange={(event) => setCapacity(Number(event.target.value))}
              onKeyDown={submitOnEnter(addBook)}
              type="number"
            />
          </label>
          <ActionButton icon={<Plus size={16} />} label="Add" busy={loading === "book"} onClick={addBook} />
        </div>
        <EditableBookTable books={props.classPeriod?.books ?? []} onUpdate={updateBook} onDelete={confirmDeleteBook} />
      </Panel>

      <Panel title="Students" icon={<Users size={18} />} wide>
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

      <NoticeModal notice={notice} onDismiss={() => setNotice(null)} />
      <ConfirmationDialog confirmation={confirmation} onCancel={() => setConfirmation(null)} />
    </section>
  );
}
