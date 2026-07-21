import type { ApiClient } from "./client";
import type { AssignmentRun, ClassAssignmentGrid, ClassPeriod, TeacherAssignmentGrid } from "../types";

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
  status: "COMPLETE",
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

export const mockClient: ApiClient = {
  registerTeacher: async () => ({ teacherId: "teacher-demo" }),
  loginTeacher: async () => ({ token: "mock-token" }),
  listClassPeriods: async () => ({
    classes: [{ id: mockClass.id, name: mockClass.name, joinCode: mockClass.joinCode }]
  }),
  createClassPeriod: async () => ({
    classId: mockClass.id,
    joinCode: mockClass.joinCode
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
    ],
    studentRankings: [
      {
        studentId: "student-1",
        rankings: [
          { bookId: "book-1", rank: 1 },
          { bookId: "book-2", rank: 2 },
          { bookId: "book-3", rank: 3 }
        ]
      },
      {
        studentId: "student-2",
        rankings: [
          { bookId: "book-2", rank: 1 },
          { bookId: "book-1", rank: 2 },
          { bookId: "book-3", rank: 3 }
        ]
      },
      {
        studentId: "student-3",
        rankings: [
          { bookId: "book-2", rank: 1 },
          { bookId: "book-3", rank: 2 },
          { bookId: "book-1", rank: 3 }
        ]
      }
    ]
  }),
  reassignStudent: async (_token, _classId, studentId, bookId) => ({
    ...mockRun,
    results: [
      { studentId: "student-1", bookId: studentId === "student-1" ? bookId : "book-1" },
      { studentId: "student-2", bookId: studentId === "student-2" ? bookId : "book-2" },
      { studentId: "student-3", bookId: studentId === "student-3" ? bookId : "book-3" }
    ],
    studentRankings: [
      {
        studentId: "student-1",
        rankings: [
          { bookId: "book-1", rank: 1 },
          { bookId: "book-2", rank: 2 },
          { bookId: "book-3", rank: 3 }
        ]
      },
      {
        studentId: "student-2",
        rankings: [
          { bookId: "book-2", rank: 1 },
          { bookId: "book-1", rank: 2 },
          { bookId: "book-3", rank: 3 }
        ]
      },
      {
        studentId: "student-3",
        rankings: [
          { bookId: "book-2", rank: 1 },
          { bookId: "book-3", rank: 2 },
          { bookId: "book-1", rank: 3 }
        ]
      }
    ]
  }),
  getAssignmentHistory: async () => ({ runs: [mockRun] }),
  getClassAssignmentGrid: async () => mockClassAssignmentGrid,
  getTeacherAssignmentGrid: async () => mockTeacherAssignmentGrid
};
