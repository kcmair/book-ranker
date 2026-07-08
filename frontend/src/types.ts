export type Book = {
  id: string;
  title: string;
  capacity: number;
};

export type Student = {
  id: string;
  username: string;
};

export type ClassPeriod = {
  id: string;
  name: string;
  joinCode: string;
  minimumRankingCount: number;
  minimumRankingCountExplicit: boolean;
  books: Book[];
  students: Student[];
};

export type AssignmentMetrics = {
  totalCost: number;
  satisfactionScore: number;
  firstChoiceCount: number;
  topThreeCount: number;
  worseThanThirdCount: number;
  unassignedStudentCount: number;
};

export type AssignmentRun = AssignmentMetrics & {
  runId: string;
  createdAt?: string;
  status: string;
};

export type AssignmentResult = {
  studentId: string;
  bookId: string;
};

export type AssignmentResults = AssignmentMetrics & {
  runId: string;
  results: AssignmentResult[];
};

export type ClassAssignmentGridRow = {
  bookTitle: string;
  students: string[];
};

export type ClassAssignmentGrid = {
  classId?: string;
  className: string;
  joinCode: string;
  assignmentRunId?: string | null;
  rows: ClassAssignmentGridRow[];
};

export type TeacherAssignmentGridColumn = {
  key?: string;
  label?: string;
  classId?: string;
  className?: string;
};

export type TeacherAssignmentGridRow = {
  bookTitle: string;
  cells: Record<string, string>;
};

export type TeacherAssignmentGrid = {
  columns: TeacherAssignmentGridColumn[];
  rows: TeacherAssignmentGridRow[];
};

export type StudentStatus = {
  submitted: boolean;
  rankCount: number;
  totalBooks: number;
  minimumRankingCount: number;
};

export type RankingItem = {
  bookId: string;
  rank: number;
};
