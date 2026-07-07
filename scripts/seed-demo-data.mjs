const baseUrl = process.env.BOOKRANKER_API_BASE_URL ?? "http://127.0.0.1:8080";
const email = process.env.BOOKRANKER_SEED_EMAIL ?? "seed.teacher@bookranker.test";
const password = process.env.BOOKRANKER_SEED_PASSWORD ?? "SeedPass123!";

const bookTitles = [
  "The Hobbit",
  "1984",
  "A Wrinkle in Time",
  "Esperanza Rising",
  "The Giver",
  "The Westing Game"
];
const capacityPerBook = 5;
const fullClassCapacity = bookTitles.length * capacityPerBook;
const classConfigs = [
  { name: "English 1", studentCount: fullClassCapacity + 2 },
  { name: "English 2", studentCount: fullClassCapacity - 2 },
  { name: "English 3", studentCount: fullClassCapacity - 3 },
  { name: "English 4", studentCount: fullClassCapacity }
];

async function request(path, method, body, token, allowedStatuses = [200]) {
  const headers = { "Content-Type": "application/json" };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${baseUrl}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  const text = await response.text();
  const data = parseBody(text);

  if (!allowedStatuses.includes(response.status)) {
    throw new Error(`${method} ${path} failed with ${response.status}: ${text}`);
  }

  return { status: response.status, data };
}

function parseBody(text) {
  if (!text) {
    return null;
  }

  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function hashString(value) {
  let hash = 2166136261;

  for (let index = 0; index < value.length; index += 1) {
    hash ^= value.charCodeAt(index);
    hash = Math.imul(hash, 16777619);
  }

  return hash >>> 0;
}

function mulberry32(seed) {
  return function random() {
    seed += 0x6d2b79f5;
    let value = seed;
    value = Math.imul(value ^ (value >>> 15), value | 1);
    value ^= value + Math.imul(value ^ (value >>> 7), value | 61);
    return ((value ^ (value >>> 14)) >>> 0) / 4294967296;
  };
}

function shuffle(items, random) {
  const shuffled = [...items];

  for (let index = shuffled.length - 1; index > 0; index -= 1) {
    const swapIndex = Math.floor(random() * (index + 1));
    [shuffled[index], shuffled[swapIndex]] = [shuffled[swapIndex], shuffled[index]];
  }

  return shuffled;
}

function rankingsFor(className, studentIndex, books) {
  const random = mulberry32(hashString(`${className}:${studentIndex}:book-rankings`));

  return shuffle(books, random).map((book, index) => {
    return { bookId: book.id, rank: index + 1 };
  });
}

function studentUsername(className, index) {
  return `${className.toLowerCase().replace(/\s+/g, "-")}-student-${String(index + 1).padStart(2, "0")}`;
}

function seededStudentIndex(className, username) {
  const prefix = `${className.toLowerCase().replace(/\s+/g, "-")}-student-`;

  if (!username.startsWith(prefix)) {
    return null;
  }

  const parsed = Number(username.slice(prefix.length));
  return Number.isInteger(parsed) && parsed > 0 ? parsed - 1 : null;
}

async function ensureTeacher() {
  await request("/api/teachers/register", "POST", { email, password }, undefined, [200, 409]);
  const { data } = await request("/api/teachers/login", "POST", { email, password });
  return data.token;
}

async function ensureClass(token, className) {
  const listed = await request("/api/classes", "GET", undefined, token);
  const existing = listed.data.classes.find((classPeriod) => classPeriod.name === className);

  if (existing) {
    return request(`/api/classes/${existing.id}`, "GET", undefined, token).then((result) => result.data);
  }

  const created = await request("/api/classes", "POST", { name: className }, token);
  return request(`/api/classes/${created.data.classId}`, "GET", undefined, token).then((result) => result.data);
}

async function ensureBooks(token, classPeriod) {
  const booksByTitle = new Map(classPeriod.books.map((book) => [book.title, book]));

  for (const title of bookTitles) {
    if (!booksByTitle.has(title)) {
      await request(
        `/api/classes/${classPeriod.id}/books`,
        "POST",
        { title, capacity: capacityPerBook },
        token
      );
    }
  }

  const refreshed = await request(`/api/classes/${classPeriod.id}`, "GET", undefined, token);
  return refreshed.data.books.filter((book) => bookTitles.includes(book.title));
}

async function loadClass(token, classId) {
  return request(`/api/classes/${classId}`, "GET", undefined, token).then((result) => result.data);
}

async function trimSeededStudents(token, classPeriod, targetStudentCount) {
  const details = await loadClass(token, classPeriod.id);
  let skipped = 0;

  for (const student of details.students) {
    const index = seededStudentIndex(classPeriod.name, student.username);

    if (index !== null && index >= targetStudentCount) {
      try {
        await request(
          `/api/classes/${classPeriod.id}/students/${student.id}`,
          "DELETE",
          undefined,
          token
        );
      } catch {
        skipped += 1;
      }
    }
  }

  return skipped;
}

async function ensureStudentsAndRankings(token, classPeriod, books, targetStudentCount) {
  const students = [];
  const skippedDeletes = await trimSeededStudents(token, classPeriod, targetStudentCount);
  const refreshed = await loadClass(token, classPeriod.id);
  const studentsByUsername = new Map(refreshed.students.map((student) => [student.username, student]));

  for (let index = 0; index < targetStudentCount; index += 1) {
    const username = studentUsername(classPeriod.name, index);
    const existing = studentsByUsername.get(username);
    const studentId = existing?.id
        ?? (await request("/api/classes/join", "POST", { joinCode: classPeriod.joinCode, username })).data.studentId;

    await request(
      `/api/students/${studentId}/rankings`,
      "POST",
      { rankings: rankingsFor(classPeriod.name, index, books) }
    );
    students.push({ id: studentId, username });
  }

  const finalDetails = await loadClass(token, classPeriod.id);
  const actualSeededStudents = finalDetails.students
    .filter((student) => seededStudentIndex(classPeriod.name, student.username) !== null)
    .length;

  return { students, skippedDeletes, actualSeededStudents };
}

async function main() {
  const token = await ensureTeacher();
  const seededClasses = [];

  for (const classConfig of classConfigs) {
    const classPeriod = await ensureClass(token, classConfig.name);
    const books = await ensureBooks(token, classPeriod);
    const { students, skippedDeletes, actualSeededStudents } = await ensureStudentsAndRankings(
      token,
      classPeriod,
      books,
      classConfig.studentCount
    );

    seededClasses.push({
      className: classPeriod.name,
      classId: classPeriod.id,
      joinCode: classPeriod.joinCode,
      books: books.length,
      targetStudents: students.length,
      actualSeededStudents,
      capacityDelta: actualSeededStudents - fullClassCapacity,
      skippedDeletes
    });
  }

  console.log(JSON.stringify({
    apiBaseUrl: baseUrl,
    teacher: { email, password },
    booksPerClass: bookTitles.length,
    capacityPerBook,
    fullClassCapacity,
    classes: seededClasses
  }, null, 2));
}

main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
