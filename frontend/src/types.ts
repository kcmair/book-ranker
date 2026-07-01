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

export type StudentStatus = {
  submitted: boolean;
  rankCount: number;
  totalBooks: number;
};

export type RankingItem = {
  bookId: string;
  rank: number;
};
