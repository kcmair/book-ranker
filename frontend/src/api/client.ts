import type {
  AssignmentResults,
  AssignmentRun,
  Book,
  ClassAssignmentGrid,
  ClassPeriod,
  RankingItem,
  StudentStatus,
  TeacherAssignmentGrid
} from "../types";

type RequestOptions = {
  token?: string;
  body?: unknown;
};

type ApiErrorShape = {
  error?: unknown;
  errors?: unknown;
  message?: unknown;
  path?: unknown;
  status?: unknown;
  title?: unknown;
  timestamp?: unknown;
};

export class ApiRequestError extends Error {
  details: string[];
  method: string;
  path: string;
  status: number;

  constructor(message: string, status: number, method: string, path: string, details: string[] = []) {
    super(message);
    this.name = "ApiRequestError";
    this.status = status;
    this.method = method;
    this.path = path;
    this.details = details;
  }
}

type CreateClassPeriodResponse = {
  classId: string;
  joinCode: string;
};

type JoinClassPeriodResponse = {
  studentId: string;
  classId: string;
  className?: string;
  existingMember: boolean;
};

type SubmitRankingsResponse = {
  status: string;
};

type RunAssignmentResponse = {
  assignmentRunId: string;
  status: string;
  totalCost: number;
  satisfactionScore: number;
  firstChoiceCount: number;
  topThreeCount: number;
  worseThanThirdCount: number;
  unassignedStudentCount: number;
};

const baseUrl = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";
const useMocks = import.meta.env.VITE_USE_MOCKS === "true";

async function request<T>(path: string, method: string, options: RequestOptions = {}): Promise<T> {
  const headers = new Headers({ "Content-Type": "application/json" });

  if (options.token) {
    headers.set("Authorization", `Bearer ${options.token}`);
  }

  const response = await fetch(`${baseUrl}${path}`, {
    method,
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined
  });

  if (!response.ok) {
    throw await buildApiRequestError(response, method, path);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

async function buildApiRequestError(response: Response, method: string, path: string) {
  const rawBody = await response.text();
  const parsedBody = parseJsonBody(rawBody);
  const { message, details } = formatApiError(parsedBody, rawBody, response.status, method, path);

  return new ApiRequestError(message, response.status, method, path, details);
}

function parseJsonBody(rawBody: string) {
  if (!rawBody.trim()) {
    return null;
  }

  try {
    return JSON.parse(rawBody) as unknown;
  } catch {
    return null;
  }
}

function formatApiError(parsedBody: unknown, rawBody: string, status: number, method: string, path: string) {
  if (!isApiErrorShape(parsedBody)) {
    return {
      message: rawBody.trim() || `${method} ${path} failed with ${status}`,
      details: [`Request: ${method} ${path}`, `HTTP status: ${status}`]
    };
  }

  const apiMessage = getString(parsedBody.message);
  const apiError = getString(parsedBody.error);
  const apiTitle = getString(parsedBody.title);
  const messageBase = apiMessage || apiError || apiTitle || `${method} ${path} failed`;
  const message = status ? `${messageBase} (${status})` : messageBase;
  const responsePath = getString(parsedBody.path);
  const details = [`Request: ${method} ${path}`];

  if (responsePath && responsePath !== path) {
    details.push(`Response path: ${responsePath}`);
  }

  const validationDetails = formatValidationErrors(parsedBody.errors);
  details.push(...validationDetails);

  return { message, details };
}

function isApiErrorShape(value: unknown): value is ApiErrorShape {
  return typeof value === "object" && value !== null;
}

function getString(value: unknown) {
  return typeof value === "string" && value.trim() ? value.trim() : "";
}

function formatValidationErrors(errors: unknown) {
  if (!Array.isArray(errors)) {
    return [];
  }

  return errors
    .map((error) => {
      if (typeof error === "string") {
        return error;
      }

      if (!isApiErrorShape(error)) {
        return "";
      }

      const field = getString((error as { field?: unknown }).field);
      const message = getString(error.message) || getString((error as { defaultMessage?: unknown }).defaultMessage);

      if (field && message) {
        return `${field}: ${message}`;
      }

      return message || field;
    })
    .filter(Boolean);
}

const liveClient = {
  registerTeacher: (email: string, password: string) =>
    request<{ teacherId: string }>("/api/teachers/register", "POST", { body: { email, password } }),
  loginTeacher: (email: string, password: string) =>
    request<{ token: string }>("/api/teachers/login", "POST", { body: { email, password } }),
  listClassPeriods: (token: string) =>
    request<{ classes: Pick<ClassPeriod, "id" | "name" | "joinCode">[] }>("/api/classes", "GET", { token }),
  createClassPeriod: (token: string, name: string) =>
    request<CreateClassPeriodResponse>("/api/classes", "POST", { token, body: { name } }),
  getClassPeriod: (token: string, classId: string) => request<ClassPeriod>(`/api/classes/${classId}`, "GET", { token }),
  updateClassPeriod: (token: string, classId: string, name: string, minimumRankingCount?: number) =>
    request<Pick<ClassPeriod, "id" | "name" | "joinCode">>(`/api/classes/${classId}`, "PATCH", {
      token,
      body: { name, minimumRankingCount }
    }),
  deleteClassPeriod: (token: string, classId: string) => request<void>(`/api/classes/${classId}`, "DELETE", { token }),
  addBook: (token: string, classId: string, title: string, capacity: number) =>
    request<{ bookId: string }>(`/api/classes/${classId}/books`, "POST", {
      token,
      body: { title, capacity }
    }),
  getBooks: (token: string, classId: string) =>
    request<{ books: Book[] }>(`/api/classes/${classId}/books`, "GET", { token }),
  updateBook: (token: string, classId: string, bookId: string, title: string, capacity: number) =>
    request<Book>(`/api/classes/${classId}/books/${bookId}`, "PATCH", {
      token,
      body: { title, capacity }
    }),
  deleteBook: (token: string, classId: string, bookId: string) =>
    request<void>(`/api/classes/${classId}/books/${bookId}`, "DELETE", { token }),
  joinClassPeriod: (joinCode: string, username: string) =>
    request<JoinClassPeriodResponse>("/api/classes/join", "POST", { body: { joinCode, username } }),
  getStudentBooks: (studentId: string) =>
    request<{ className?: string; minimumRankingCount?: number; books: Book[] }>(
      `/api/students/${studentId}/books`,
      "GET"
    ),
  updateStudent: (token: string, classId: string, studentId: string, username: string) =>
    request<{ id: string; username: string }>(`/api/classes/${classId}/students/${studentId}`, "PATCH", {
      token,
      body: { username }
    }),
  deleteStudent: (token: string, classId: string, studentId: string) =>
    request<void>(`/api/classes/${classId}/students/${studentId}`, "DELETE", { token }),
  clearClassStudents: (token: string, classId: string) =>
    request<void>(`/api/classes/${classId}/students`, "DELETE", { token }),
  submitRankings: (studentId: string, rankings: RankingItem[]) =>
    request<SubmitRankingsResponse>(`/api/students/${studentId}/rankings`, "POST", {
      body: { rankings }
    }),
  getStudentStatus: (studentId: string) => request<StudentStatus>(`/api/students/${studentId}/status`, "GET"),
  runAssignment: (token: string, classId: string) =>
    request<RunAssignmentResponse>(`/api/classes/${classId}/assign`, "POST", { token }),
  getLatestAssignment: (token: string, classId: string) =>
    request<AssignmentResults>(`/api/classes/${classId}/assignments/latest`, "GET", { token }),
  getAssignmentHistory: (token: string, classId: string) =>
    request<{ runs: AssignmentRun[] }>(`/api/classes/${classId}/assignments`, "GET", { token }),
  getClassAssignmentGrid: (joinCode: string) =>
    request<ClassAssignmentGrid>(`/api/public/classes/${encodeURIComponent(joinCode)}/assignment-grid`, "GET"),
  getTeacherAssignmentGrid: (token: string) =>
    request<TeacherAssignmentGrid>("/api/teachers/me/assignment-grid", "GET", { token })
};

const mockClass: ClassPeriod = {
  id: "class-demo",
  name: "Period 3 Reading",
  joinCode: "RANK42",
  minimumRankingCount: 3,
  minimumRankingCountExplicit: false,
  books: [
    { id: "book-1", title: "A Wrinkle in Time", capacity: 3 },
    { id: "book-2", title: "The Hobbit", capacity: 4 },
    { id: "book-3", title: "Esperanza Rising", capacity: 3 }
  ],
  students: [
    { id: "student-1", username: "maya" },
    { id: "student-2", username: "leo" },
    { id: "student-3", username: "nora" }
  ]
};

const mockRun: AssignmentRun = {
  runId: "run-demo",
  createdAt: new Date().toISOString(),
  status: "COMPLETED",
  totalCost: 4,
  satisfactionScore: 0.88,
  firstChoiceCount: 2,
  topThreeCount: 3,
  worseThanThirdCount: 0,
  unassignedStudentCount: 0
};

const mockClassAssignmentGrid: ClassAssignmentGrid = {
  classId: mockClass.id,
  className: mockClass.name,
  joinCode: mockClass.joinCode,
  assignmentRunId: mockRun.runId,
  rows: [
    { bookTitle: "A Wrinkle in Time", students: ["maya"] },
    { bookTitle: "The Hobbit", students: ["leo", "nora"] },
    { bookTitle: "Esperanza Rising", students: [] }
  ]
};

const mockTeacherAssignmentGrid: TeacherAssignmentGrid = {
  columns: [
    { classId: mockClass.id, className: mockClass.name },
    { classId: "class-demo-2", className: "Period 4 Reading" }
  ],
  rows: [
    { bookTitle: "A Wrinkle in Time", cells: { [mockClass.id]: "maya", "class-demo-2": "" } },
    { bookTitle: "The Hobbit", cells: { [mockClass.id]: "leo", "class-demo-2": "jordan" } },
    { bookTitle: "", cells: { [mockClass.id]: "nora", "class-demo-2": "" } }
  ]
};

const mockClient: typeof liveClient = {
  registerTeacher: async () => ({ teacherId: "teacher-demo" }),
  loginTeacher: async () => ({ token: "mock-token" }),
  listClassPeriods: async () => ({
    classes: [{ id: mockClass.id, name: mockClass.name, joinCode: mockClass.joinCode }]
  }),
  createClassPeriod: async (_token, name) => ({
    classId: mockClass.id,
    joinCode: name ? mockClass.joinCode : mockClass.joinCode
  }),
  getClassPeriod: async () => mockClass,
  updateClassPeriod: async (_token, classId, name, minimumRankingCount) => {
    mockClass.name = name;
    if (minimumRankingCount !== undefined) {
      mockClass.minimumRankingCount = minimumRankingCount;
      mockClass.minimumRankingCountExplicit = true;
    }
    return { id: classId, name, joinCode: mockClass.joinCode };
  },
  deleteClassPeriod: async () => undefined,
  addBook: async () => ({ bookId: "book-new" }),
  getBooks: async () => ({ books: mockClass.books }),
  updateBook: async (_token, _classId, bookId, title, capacity) => ({ id: bookId, title, capacity }),
  deleteBook: async () => undefined,
  joinClassPeriod: async () => ({
    studentId: "student-demo",
    classId: mockClass.id,
    className: mockClass.name,
    existingMember: false
  }),
  getStudentBooks: async () => ({
    className: mockClass.name,
    minimumRankingCount: mockClass.minimumRankingCount,
    books: mockClass.books
  }),
  updateStudent: async (_token, _classId, studentId, username) => ({ id: studentId, username }),
  deleteStudent: async () => undefined,
  clearClassStudents: async () => undefined,
  submitRankings: async () => ({ status: "submitted" }),
  getStudentStatus: async () => ({
    submitted: true,
    rankCount: mockClass.books.length,
    totalBooks: mockClass.books.length,
    minimumRankingCount: mockClass.minimumRankingCount,
    rankings: mockClass.books.map((book, index) => ({ bookId: book.id, rank: index + 1 }))
  }),
  runAssignment: async () => ({
    assignmentRunId: mockRun.runId,
    status: mockRun.status,
    totalCost: mockRun.totalCost,
    satisfactionScore: mockRun.satisfactionScore,
    firstChoiceCount: mockRun.firstChoiceCount,
    topThreeCount: mockRun.topThreeCount,
    worseThanThirdCount: mockRun.worseThanThirdCount,
    unassignedStudentCount: mockRun.unassignedStudentCount
  }),
  getLatestAssignment: async () => ({
    ...mockRun,
    results: [
      { studentId: "student-1", bookId: "book-1" },
      { studentId: "student-2", bookId: "book-2" },
      { studentId: "student-3", bookId: "book-3" }
    ]
  }),
  getAssignmentHistory: async () => ({ runs: [mockRun] }),
  getClassAssignmentGrid: async () => mockClassAssignmentGrid,
  getTeacherAssignmentGrid: async () => mockTeacherAssignmentGrid
};

export const api = useMocks ? mockClient : liveClient;
