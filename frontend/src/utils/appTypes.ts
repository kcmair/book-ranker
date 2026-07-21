export type AuthMode = "signup" | "login";

export type View = "classes" | "books" | "results";

export type Notice = { kind: "success" | "error"; message: string; details?: string[] } | null;

export type Confirmation = {
  title: string;
  message: string;
  confirmLabel?: string;
  confirmTone?: "danger" | "warning";
  onConfirm: () => void;
} | null;

export type TeacherClassSummary = {
  id: string;
  name: string;
  joinCode: string;
};

export type AssignmentSortKey = "student" | "book";

export type SortDirection = "asc" | "desc";
