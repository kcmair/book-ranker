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
import { mockClient } from "./mockClient";

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

export const liveClient = {
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

export type ApiClient = typeof liveClient;

export const api = useMocks ? mockClient : liveClient;
