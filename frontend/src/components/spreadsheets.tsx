import { Fragment, useEffect, useRef, useState } from "react";
import type { ClassAssignmentGrid, TeacherAssignmentGrid } from "../types";
import { Metric } from "./ui";

type ClassAssignmentSpreadsheetProps = {
  grid: ClassAssignmentGrid;
};

type TeacherAssignmentSpreadsheetProps = {
  grid: TeacherAssignmentGrid;
};

export function ClassAssignmentSpreadsheet({ grid }: ClassAssignmentSpreadsheetProps) {
  const [studentSearch, setStudentSearch] = useState("");
  const spreadsheetRef = useRef<HTMLDivElement>(null);
  const normalizedSearch = studentSearch.trim().toLowerCase();

  useEffect(() => {
    if (!normalizedSearch) {
      return;
    }

    const firstMatch = spreadsheetRef.current?.querySelector('[data-student-match="true"]');
    firstMatch?.scrollIntoView({ block: "center", inline: "nearest", behavior: "smooth" });
  }, [normalizedSearch]);

  return (
    <div className="spreadsheet-wrap public-assignment-grid" ref={spreadsheetRef}>
      <div className="spreadsheet-meta">
        <Metric label="Class" value={grid.className} />
      </div>
      <div className="spreadsheet-search">
        <label>
          Find your name
          <input
            value={studentSearch}
            onChange={(event) => setStudentSearch(event.target.value)}
            placeholder="Start typing your name"
          />
        </label>
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
                    {row.students.map((student, studentIndex) => {
                      const isMatch = Boolean(normalizedSearch && student.toLowerCase().includes(normalizedSearch));

                      return (
                        <Fragment key={student}>
                          <span className={isMatch ? "student-search-match" : undefined} data-student-match={isMatch}>
                            {student}
                          </span>
                          {studentIndex < row.students.length - 1 && <span className="student-separator">·</span>}
                        </Fragment>
                      );
                    })}
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

export function TeacherAssignmentSpreadsheet({ grid }: TeacherAssignmentSpreadsheetProps) {
  return (
    <div className="spreadsheet-wrap teacher-assignment-grid">
      <table className="spreadsheet-table teacher-spreadsheet">
        <thead>
          <tr>
            <th>Book</th>
            {grid.columns.map((column) => (
              <th key={column.classId}>{column.className}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {grid.rows.map((row, rowIndex) => (
            <tr key={`${row.bookTitle}-${rowIndex}`}>
              <th scope="row">{row.bookTitle}</th>
              {grid.columns.map((column) => (
                <td key={column.classId}>
                  {row.cells[column.classId] ? <span className="student-cell">{row.cells[column.classId]}</span> : ""}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
