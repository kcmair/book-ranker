const baseUrl = process.env.BOOKRANKER_API_BASE_URL ?? "http://127.0.0.1:8080";
const email = process.env.BOOKRANKER_SEED_EMAIL ?? "seed.teacher@bookranker.test";
const password = process.env.BOOKRANKER_SEED_PASSWORD ?? "SeedPass123!";

const classNames = ["English 1", "English 2", "English 3", "English 4"];
const bookTitles = [
  "The Hobbit",
  "1984",
  "A Wrinkle in Time",
  "Esperanza Rising",
  "The Giver",
  "The Westing Game"
];
const capacityPerBook = 5;
const studentsPerClass = bookTitles.length * capacityPerBook;

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

function rankingsFor(studentIndex, books) {
  const preferred = studentIndex % books.length;

  return books.map((_, offset) => {
    const bookIndex = (preferred + offset) % books.length;
    return { bookId: books[bookIndex].id, rank: offset + 1 };
  });
}

function studentUsername(className, index) {
  return `${className.toLowerCase().replace(/\s+/g, "-")}-student-${String(index + 1).padStart(2, "0")}`;
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

async function ensureStudentsAndRankings(classPeriod, books) {
  const students = [];

  for (let index = 0; index < studentsPerClass; index += 1) {
    const username = studentUsername(classPeriod.name, index);
    const joined = await request("/api/classes/join", "POST", { joinCode: classPeriod.joinCode, username });
    await request(
      `/api/students/${joined.data.studentId}/rankings`,
      "POST",
      { rankings: rankingsFor(index, books) }
    );
    students.push({ id: joined.data.studentId, username });
  }

  return students;
}

async function main() {
  const token = await ensureTeacher();
  const seededClasses = [];

  for (const className of classNames) {
    const classPeriod = await ensureClass(token, className);
    const books = await ensureBooks(token, classPeriod);
    const students = await ensureStudentsAndRankings(classPeriod, books);

    seededClasses.push({
      className: classPeriod.name,
      classId: classPeriod.id,
      joinCode: classPeriod.joinCode,
      books: books.length,
      students: students.length
    });
  }

  console.log(JSON.stringify({
    apiBaseUrl: baseUrl,
    teacher: { email, password },
    booksPerClass: bookTitles.length,
    capacityPerBook,
    studentsPerClass,
    classes: seededClasses
  }, null, 2));
}

main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
