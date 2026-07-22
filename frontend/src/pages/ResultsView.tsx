import { BarChart3, ClipboardList, RefreshCw, Trash2 } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { ApiRequestError, api } from "../api/client";
import { ActionButton, ConfirmationDialog, Metric, NoticeModal, Panel, SortButton } from "../components/ui";
import type { AssignmentResults, AssignmentRun, ClassPeriod } from "../types";
import type { AssignmentSortKey, Confirmation, Notice, SortDirection } from "../utils/appTypes";
import { withNotice } from "../utils/notices";
import { formatPercent, ordinalLabel } from "../utils/viewHelpers";

type ResultsViewProps = {
  token: string;
  classId: string;
  classPeriod: ClassPeriod | null;
  latestAssignment: AssignmentResults | null;
  onClassPeriod: (classPeriod: ClassPeriod) => void;
  onAssignment: (assignment: AssignmentResults | null) => void;
};

export function ResultsView(props: ResultsViewProps) {
  const { classId, onAssignment, onClassPeriod, token } = props;
  const [history, setHistory] = useState<AssignmentRun[]>([]);
  const [notice, setNotice] = useState<Notice>(null);
  const [confirmation, setConfirmation] = useState<Confirmation>(null);
  const [loading, setLoading] = useState("");
  const [assignmentSort, setAssignmentSort] = useState<{
    key: AssignmentSortKey;
    direction: SortDirection;
  }>({ key: "student", direction: "asc" });

  const bookNames = useMemo(
    () => new Map((props.classPeriod?.books ?? []).map((book) => [book.id, book.title])),
    [props.classPeriod?.books]
  );
  const studentNames = useMemo(
    () => new Map((props.classPeriod?.students ?? []).map((student) => [student.id, student.username])),
    [props.classPeriod?.students]
  );
  const booksById = useMemo(
    () => new Map((props.classPeriod?.books ?? []).map((book) => [book.id, book])),
    [props.classPeriod?.books]
  );
  const assignmentCountsByBookId = useMemo(() => {
    const counts = new Map<string, number>();
    for (const result of props.latestAssignment?.results ?? []) {
      counts.set(result.bookId, (counts.get(result.bookId) ?? 0) + 1);
    }

    return counts;
  }, [props.latestAssignment?.results]);
  const rankingColumnCount = useMemo(() => {
    const classBookCount = props.classPeriod?.books.length ?? 0;
    const submittedRankingCount = Math.max(
      0,
      ...(props.latestAssignment?.studentRankings ?? []).map((studentRanking) => studentRanking.rankings.length)
    );

    return Math.max(classBookCount, submittedRankingCount);
  }, [props.classPeriod?.books.length, props.latestAssignment?.studentRankings]);
  const rankingColumnLabels = useMemo(
    () => Array.from({ length: rankingColumnCount }, (_, index) => ordinalLabel(index + 1)),
    [rankingColumnCount]
  );
  const assignmentRows = useMemo(() => {
    const assignmentsByStudentId = new Map(
      (props.latestAssignment?.results ?? []).map((result) => [result.studentId, result.bookId])
    );
    const rankingsByStudentId = new Map(
      (props.latestAssignment?.studentRankings ?? []).map((studentRanking) => [
        studentRanking.studentId,
        [...studentRanking.rankings].sort((left, right) => left.rank - right.rank)
      ])
    );
    const students =
      props.classPeriod?.students ??
      (props.latestAssignment?.results ?? []).map((result) => ({
        id: result.studentId,
        username: studentNames.get(result.studentId) ?? result.studentId
      }));
    const rows = [
      ...students.map((student) => {
        const assignedBookId = assignmentsByStudentId.get(student.id);
        const submittedRankings = rankingsByStudentId.get(student.id) ?? [];

        return {
          id: student.id,
          student: student.username,
          assignedBookId,
          book: assignedBookId ? (bookNames.get(assignedBookId) ?? assignedBookId) : "Unassigned",
          rankingBookIds: submittedRankings.map((ranking) => ranking.bookId),
          rankingBooks: submittedRankings.map((ranking) => bookNames.get(ranking.bookId) ?? ranking.bookId),
          unassigned: !assignedBookId
        };
      })
    ];

    return [...rows].sort((left, right) => {
      const primaryCompare = left[assignmentSort.key].localeCompare(right[assignmentSort.key], undefined, {
        sensitivity: "base"
      });
      const stableCompare = left.student.localeCompare(right.student, undefined, { sensitivity: "base" });
      const compare = primaryCompare || stableCompare;

      return assignmentSort.direction === "asc" ? compare : -compare;
    });
  }, [
    assignmentSort,
    bookNames,
    props.classPeriod?.students,
    props.latestAssignment?.results,
    props.latestAssignment?.studentRankings,
    studentNames
  ]);

  function toggleAssignmentSort(key: AssignmentSortKey) {
    setAssignmentSort((currentSort) => ({
      key,
      direction: currentSort.key === key && currentSort.direction === "asc" ? "desc" : "asc"
    }));
  }

  const loadResultsState = useCallback(async () => {
    const [details, runs] = await Promise.all([
      api.getClassPeriod(token, classId),
      api.getAssignmentHistory(token, classId)
    ]);
    onClassPeriod(details);
    setHistory(runs.runs);

    try {
      onAssignment(await api.getLatestAssignment(token, classId));
    } catch (error) {
      if (error instanceof ApiRequestError && error.status === 404) {
        onAssignment(null);
        return;
      }

      throw error;
    }
  }, [classId, onAssignment, onClassPeriod, token]);

  async function refreshResults() {
    return withNotice(setLoading, setNotice, "results", loadResultsState);
  }

  useEffect(() => {
    if (!classId) {
      return;
    }

    void withNotice(setLoading, setNotice, "results", loadResultsState);
  }, [classId, loadResultsState]);

  async function reassignStudent(studentId: string, bookId: string) {
    return withNotice(setLoading, setNotice, `reassign-${studentId}`, async () => {
      const latest = await api.reassignStudent(props.token, props.classId, studentId, bookId);
      props.onAssignment(latest);
    });
  }

  function confirmOrReassignStudent(
    studentId: string,
    studentName: string,
    currentBookId: string | undefined,
    nextBookId: string
  ) {
    if (currentBookId === nextBookId) {
      return;
    }

    const book = booksById.get(nextBookId);
    const currentCount = assignmentCountsByBookId.get(nextBookId) ?? 0;
    const assignedCountExcludingStudent = currentBookId === nextBookId ? currentCount - 1 : currentCount;

    if (book && assignedCountExcludingStudent >= book.capacity) {
      setConfirmation({
        title: "Override book capacity?",
        message: `${book.title} already has ${currentCount} of ${book.capacity} students assigned. Reassigning ${studentName} will exceed the max students for this book.`,
        confirmLabel: "Override capacity",
        confirmTone: "warning",
        onConfirm: () => {
          setConfirmation(null);
          void reassignStudent(studentId, nextBookId);
        }
      });
      return;
    }

    void reassignStudent(studentId, nextBookId);
  }

  function confirmDeleteRun(run: AssignmentRun) {
    setConfirmation({
      title: "Delete assignment run?",
      message:
        "This removes this run from history. If it is the latest run, results will fall back to the previous completed run. If no completed runs remain, students will see the ranking view again.",
      confirmLabel: "Delete run",
      onConfirm: () => {
        setConfirmation(null);
        void withNotice(setLoading, setNotice, `delete-run-${run.runId}`, async () => {
          await api.deleteAssignmentRun(props.token, props.classId, run.runId);
          await loadResultsState();
        });
      }
    });
  }

  return (
    <section className="workspace-grid results-grid">
      <Panel title="Results lookup" icon={<RefreshCw size={18} />}>
        <div className="metric-grid two">
          <Metric label="Class" value={props.classPeriod?.name ?? "No class selected"} />
          <Metric label="Link ID" value={props.classPeriod?.joinCode ?? "-"} />
        </div>
        <ActionButton
          icon={<RefreshCw size={16} />}
          label="Refresh"
          busy={loading === "results"}
          onClick={refreshResults}
        />
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
        <table className="assignment-results-table">
          <thead>
            <tr>
              <th>
                <SortButton
                  label="Student"
                  active={assignmentSort.key === "student"}
                  onClick={() => toggleAssignmentSort("student")}
                />
              </th>
              <th>
                <SortButton
                  label="Assigned book"
                  active={assignmentSort.key === "book"}
                  onClick={() => toggleAssignmentSort("book")}
                />
              </th>
              {rankingColumnLabels.map((label) => (
                <th key={label}>{label}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {assignmentRows.map((row) => (
              <tr key={row.id}>
                <td>{row.student}</td>
                <td className={row.unassigned ? "danger-text" : undefined}>
                  <select
                    aria-label={`Assigned book for ${row.student}`}
                    className="assignment-select"
                    disabled={loading === `reassign-${row.id}` || !props.latestAssignment}
                    value={row.assignedBookId ?? ""}
                    onChange={(event) => {
                      const bookId = event.target.value;
                      if (bookId) {
                        confirmOrReassignStudent(row.id, row.student, row.assignedBookId, bookId);
                      }
                    }}
                  >
                    <option disabled value="">
                      Unassigned
                    </option>
                    {(props.classPeriod?.books ?? []).map((book) => (
                      <option key={book.id} value={book.id}>
                        {book.title}
                      </option>
                    ))}
                  </select>
                </td>
                {rankingColumnLabels.map((label, index) => {
                  const rankedBookId = row.rankingBookIds[index];
                  const rankedBook = row.rankingBooks[index] ?? "";
                  const isAssignedRanking = Boolean(row.assignedBookId && rankedBookId === row.assignedBookId);

                  return (
                    <td className={isAssignedRanking ? "assigned-ranking-cell" : undefined} key={`${row.id}-${label}`}>
                      {rankedBook || <span className="empty-cell">-</span>}
                    </td>
                  );
                })}
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
              <th>Satisfaction</th>
              <th>Created</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {history.map((run) => (
              <tr key={run.runId}>
                <td>{run.runId}</td>
                <td>{run.status}</td>
                <td>{formatPercent(run.satisfactionScore)}</td>
                <td>{run.createdAt ? new Date(run.createdAt).toLocaleString() : "-"}</td>
                <td>
                  <div className="table-actions">
                    <button
                      type="button"
                      aria-label={`Delete assignment run ${run.runId}`}
                      disabled={loading === `delete-run-${run.runId}`}
                      onClick={() => confirmDeleteRun(run)}
                    >
                      <Trash2 size={15} />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Panel>

      <NoticeModal notice={notice} onDismiss={() => setNotice(null)} />
      <ConfirmationDialog confirmation={confirmation} onCancel={() => setConfirmation(null)} />
    </section>
  );
}
