import { Check, Edit3, Trash2 } from "lucide-react";
import { useState } from "react";
import type { Book, ClassPeriod } from "../types";
import { submitOnEnter } from "../utils/viewHelpers";

type EditableBookTableProps = {
  books: Book[];
  onUpdate: (book: Book, title: string, capacity: number) => void;
  onDelete: (book: Book) => void;
};

type EditableStudentTableProps = {
  students: ClassPeriod["students"];
  onUpdate: (student: ClassPeriod["students"][number], username: string) => void;
  onDelete: (student: ClassPeriod["students"][number]) => void;
};

export function EditableBookTable({ books, onUpdate, onDelete }: EditableBookTableProps) {
  const [editingBookId, setEditingBookId] = useState("");
  const [title, setTitle] = useState("");
  const [capacity, setCapacity] = useState(1);

  return (
    <table>
      <thead>
        <tr>
          <th>Title</th>
          <th>Max students</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {books.map((book) => (
          <tr key={book.id}>
            <td>
              {editingBookId === book.id ? (
                <input
                  value={title}
                  onChange={(event) => setTitle(event.target.value)}
                  onKeyDown={submitOnEnter(() => {
                    onUpdate(book, title, capacity);
                    setEditingBookId("");
                  })}
                  aria-label={`Title for ${book.title}`}
                />
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
                  onKeyDown={submitOnEnter(() => {
                    onUpdate(book, title, capacity);
                    setEditingBookId("");
                  })}
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

export function EditableStudentTable({ students, onUpdate, onDelete }: EditableStudentTableProps) {
  const [editingStudentId, setEditingStudentId] = useState("");
  const [username, setUsername] = useState("");

  return (
    <table>
      <thead>
        <tr>
          <th>Username</th>
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
                  onKeyDown={submitOnEnter(() => {
                    onUpdate(student, username);
                    setEditingStudentId("");
                  })}
                  aria-label={`Username for ${student.username}`}
                />
              ) : (
                student.username
              )}
            </td>
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
