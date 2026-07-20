import { BookOpen, Check, ClipboardList, Edit3, Plus, RefreshCw, Trash2, Users } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { api } from "../api/client";
import { TeacherAssignmentSpreadsheet } from "../components/spreadsheets";
import { ActionButton, ConfirmationDialog, NoticeModal, Panel } from "../components/ui";
import type { ClassPeriod, TeacherAssignmentGrid } from "../types";
import type { Confirmation, Notice, TeacherClassSummary } from "../utils/appTypes";
import { withNotice } from "../utils/notices";
import { submitOnEnter } from "../utils/viewHelpers";

type TeacherLandingProps = {
  token: string;
  onOpenClass: (classPeriod: ClassPeriod) => void;
};

export function TeacherLanding(props: TeacherLandingProps) {
  const [classes, setClasses] = useState<TeacherClassSummary[]>([]);
  const [className, setClassName] = useState("");
  const [editingClassId, setEditingClassId] = useState("");
  const [editingClassName, setEditingClassName] = useState("");
  const [teacherGrid, setTeacherGrid] = useState<TeacherAssignmentGrid | null>(null);
  const [notice, setNotice] = useState<Notice>(null);
  const [loading, setLoading] = useState("");
  const [confirmation, setConfirmation] = useState<Confirmation>(null);

  const refreshClasses = useCallback(async () => {
    return withNotice(setLoading, setNotice, "classes", async () => {
      const response = await api.listClassPeriods(props.token);
      setClasses(response.classes);
    });
  }, [props.token]);

  useEffect(() => {
    void refreshClasses();
  }, [refreshClasses]);

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
    });
  }

  return (
    <section className="teacher-home">
      <Panel title="Create class" icon={<Plus size={18} />}>
        <div className="form-grid">
          <label>
            Class name
            <input
              value={className}
              onChange={(event) => setClassName(event.target.value)}
              onKeyDown={submitOnEnter(createClassPeriod)}
            />
          </label>
        </div>
        <ActionButton
          icon={<Plus size={16} />}
          label="Create class"
          busy={loading === "class"}
          onClick={createClassPeriod}
        />
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
                  <input
                    value={editingClassName}
                    onChange={(event) => setEditingClassName(event.target.value)}
                    onKeyDown={submitOnEnter(() => updateClass(classItem))}
                  />
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

      <NoticeModal notice={notice} onDismiss={() => setNotice(null)} />
      <ConfirmationDialog confirmation={confirmation} onCancel={() => setConfirmation(null)} />
    </section>
  );
}
