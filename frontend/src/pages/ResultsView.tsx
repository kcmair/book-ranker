import { BarChart3, ClipboardList, RefreshCw } from "lucide-react";
import { useMemo, useState } from "react";
import { api } from "../api/client";
import { ActionButton, Metric, NoticeModal, Panel, SortButton } from "../components/ui";
import type { AssignmentResults, AssignmentRun, ClassPeriod } from "../types";
import type { AssignmentSortKey, Notice, SortDirection } from "../utils/appTypes";
import { withNotice } from "../utils/notices";
import { formatPercent, ordinalLabel } from "../utils/viewHelpers";

type ResultsViewProps = {
  token: string;
  classId: string;
  classPeriod: ClassPeriod | null;
  latestAssignment: AssignmentResults | null;
  onClassPeriod: (classPeriod: ClassPeriod) => void;
  onAssignment: (assignment: AssignmentResults) => void;
};

export function ResultsView(props: ResultsViewProps) {
  const [history, setHistory] = useState<AssignmentRun[]>([]);
  const [notice, setNotice] = useState<Notice>(null);
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
        <table>
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
                <td className={row.unassigned ? "danger-text" : undefined}>{row.book}</td>
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

      <NoticeModal notice={notice} onDismiss={() => setNotice(null)} />
    </section>
  );
}
