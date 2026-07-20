import {
  closestCenter,
  DragOverlay,
  DndContext,
  type DragEndEvent,
  type DragStartEvent,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors
} from "@dnd-kit/core";
import { arrayMove, sortableKeyboardCoordinates } from "@dnd-kit/sortable";
import { BarChart3, BookOpen, Check, ClipboardList, Loader2, Send, UserPlus } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { api } from "../api/client";
import {
  AVAILABLE_BOOKS_DROPZONE_ID,
  AvailableBooksDropZone,
  RANKED_BOOKS_DROPZONE_ID,
  RankingDragOverlay,
  RankingDropZone
} from "../components/rankingDnd";
import { ClassAssignmentSpreadsheet } from "../components/spreadsheets";
import { ActionButton, Metric, NoticeModal, Panel } from "../components/ui";
import type { Book, ClassAssignmentGrid, RankingItem, StudentStatus } from "../types";
import type { Notice } from "../utils/appTypes";
import { withNotice } from "../utils/notices";
import { getPollJoinCode, submitOnEnter } from "../utils/viewHelpers";

function orderedRankingBookIds(status: StudentStatus, classBooks: Book[]) {
  return [...(status.rankings ?? [])]
    .sort((left, right) => left.rank - right.rank)
    .map((ranking) => ranking.bookId)
    .filter((bookId) => classBooks.some((book) => book.id === bookId));
}

function moveRankingBook(currentRankedBookIds: string[], books: Book[], activeBookId: string, overId: string) {
  const activeIndex = currentRankedBookIds.indexOf(activeBookId);
  const overIndex = currentRankedBookIds.indexOf(overId);

  if (activeIndex >= 0 && overIndex >= 0) {
    return arrayMove(currentRankedBookIds, activeIndex, overIndex);
  }

  if (activeIndex >= 0 && overId === AVAILABLE_BOOKS_DROPZONE_ID) {
    return currentRankedBookIds.filter((bookId) => bookId !== activeBookId);
  }

  if (activeIndex >= 0 || !books.some((book) => book.id === activeBookId)) {
    return currentRankedBookIds;
  }

  if (overId === RANKED_BOOKS_DROPZONE_ID) {
    return [...currentRankedBookIds, activeBookId];
  }

  if (overIndex >= 0) {
    const nextRankedBookIds = [...currentRankedBookIds];
    nextRankedBookIds.splice(overIndex, 0, activeBookId);
    return nextRankedBookIds;
  }

  return currentRankedBookIds;
}

export function StudentPoll() {
  const urlJoinCode = getPollJoinCode();
  const hasUrlJoinCode = Boolean(urlJoinCode);
  const [joinCode, setJoinCode] = useState(urlJoinCode);
  const [publicAssignmentGrid, setPublicAssignmentGrid] = useState<ClassAssignmentGrid | null>(null);
  const [assignmentGridChecked, setAssignmentGridChecked] = useState(!hasUrlJoinCode);
  const [username, setUsername] = useState("");
  const [studentId, setStudentId] = useState("");
  const [classDisplayName, setClassDisplayName] = useState("");
  const [books, setBooks] = useState<Book[]>([]);
  const [minimumRankingCount, setMinimumRankingCount] = useState(0);
  const [notice, setNotice] = useState<Notice>(null);
  const [loading, setLoading] = useState("");
  const [status, setStatus] = useState<StudentStatus | null>(null);
  const [rankedBookIds, setRankedBookIds] = useState<string[]>([]);
  const [activeDragBookId, setActiveDragBookId] = useState("");
  const [existingMember, setExistingMember] = useState(false);
  const hasPreviousRankings = Boolean(status && status.rankCount > 0);
  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: { distance: 6 }
    }),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates
    })
  );
  const availableBooks = useMemo(
    () => books.filter((book) => !rankedBookIds.includes(book.id)),
    [books, rankedBookIds]
  );
  const rankedBooks = useMemo(
    () =>
      rankedBookIds
        .map((bookId) => books.find((book) => book.id === bookId))
        .filter((book): book is Book => Boolean(book)),
    [books, rankedBookIds]
  );
  const activeDragBook = useMemo(
    () => books.find((book) => book.id === activeDragBookId) ?? null,
    [activeDragBookId, books]
  );

  useEffect(() => {
    let active = true;

    if (!urlJoinCode) {
      setAssignmentGridChecked(true);
      return;
    }

    setAssignmentGridChecked(false);
    setPublicAssignmentGrid(null);

    api
      .getClassAssignmentGrid(urlJoinCode)
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
      setClassDisplayName(response.className ?? bookResponse.className ?? (joinCode || response.classId));
      setStatus(nextStatus);
      setBooks(bookResponse.books);
      setMinimumRankingCount(bookResponse.minimumRankingCount ?? bookResponse.books.length);
      setRankedBookIds(orderedRankingBookIds(nextStatus, bookResponse.books));
      setExistingMember(response.existingMember);
    });
  }

  async function submitRankings() {
    return withNotice(setLoading, setNotice, "rankings", async () => {
      const payload: RankingItem[] = rankedBookIds.map((bookId, index) => ({ bookId, rank: index + 1 }));
      if (payload.length < minimumRankingCount) {
        setNotice({ kind: "error", message: `Rank at least ${minimumRankingCount} books before submitting.` });
        return;
      }
      await api.submitRankings(studentId, payload);
      const nextStatus = await api.getStudentStatus(studentId);
      setStatus(nextStatus);
      setNotice({ kind: "success", message: "Rankings submitted." });
    });
  }

  function handleRankingDragStart(event: DragStartEvent) {
    setActiveDragBookId(String(event.active.id));
  }

  function handleRankingDragEnd(event: DragEndEvent) {
    const activeBookId = String(event.active.id);
    const overId = event.over?.id ? String(event.over.id) : "";

    setActiveDragBookId("");

    if (!overId) {
      return;
    }

    setRankedBookIds((currentRankedBookIds) => moveRankingBook(currentRankedBookIds, books, activeBookId, overId));
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
                  <input
                    value={joinCode}
                    onChange={(event) => setJoinCode(event.target.value.toUpperCase())}
                    onKeyDown={submitOnEnter(joinClassPeriod)}
                  />
                </label>
              )}
              <label>
                Username
                <input
                  value={username}
                  onChange={(event) => setUsername(event.target.value)}
                  onKeyDown={submitOnEnter(joinClassPeriod)}
                />
              </label>
            </div>
            <ActionButton
              icon={<Send size={16} />}
              label="Continue"
              busy={loading === "join"}
              onClick={joinClassPeriod}
            />
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
                This student is already a member of this class. You can revise the rankings below; submitting again will
                replace the old rankings.
              </div>
            )}
            <div className="metric-grid two">
              <Metric label="Books" value={books.length} />
              <Metric label="Rankings required" value={minimumRankingCount} />
            </div>
            <DndContext
              sensors={sensors}
              collisionDetection={closestCenter}
              onDragStart={handleRankingDragStart}
              onDragEnd={handleRankingDragEnd}
              onDragCancel={() => setActiveDragBookId("")}
            >
              <div className="ranking-board">
                <AvailableBooksDropZone books={availableBooks} />
                <RankingDropZone books={rankedBooks} />
              </div>
              <DragOverlay>{activeDragBook && <RankingDragOverlay book={activeDragBook} />}</DragOverlay>
            </DndContext>
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
            <div className="metric-grid">
              <Metric label="Submitted" value={status.submitted ? "Yes" : "No"} />
              <Metric label="Ranked" value={`${status.rankCount}/${status.totalBooks}`} />
              <Metric label="Required" value={status.minimumRankingCount} />
            </div>
          </Panel>
        )}

        <NoticeModal notice={notice} onDismiss={() => setNotice(null)} />
      </section>
    </main>
  );
}
