package com.bookranker.assignment;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AssignmentControllerTests {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void assignmentRunRequiresAuthentication() throws Exception {
    mockMvc
        .perform(post("/api/classes/{classId}/assign", UUID.randomUUID()))
        .andExpect(status().isForbidden());
  }

  @Test
  void teacherCanRunAssignmentAndRetrieveResults() throws Exception {
    String token = registerAndLoginTeacher();
    ClassFixture fixture = createRankedClassPeriod(token);

    MvcResult runResult =
        mockMvc
            .perform(
                post("/api/classes/{classId}/assign", fixture.classId())
                    .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assignmentRunId", not(blankOrNullString())))
            .andExpect(jsonPath("$.status", equalTo("COMPLETE")))
            .andExpect(jsonPath("$.satisfactionScore", equalTo(1.0)))
            .andExpect(jsonPath("$.firstChoiceCount", equalTo(2)))
            .andExpect(jsonPath("$.topThreeCount", equalTo(2)))
            .andExpect(jsonPath("$.worseThanThirdCount", equalTo(0)))
            .andExpect(jsonPath("$.unassignedStudentCount", equalTo(0)))
            .andReturn();

    String runId =
        objectMapper
            .readTree(runResult.getResponse().getContentAsString())
            .get("assignmentRunId")
            .asText();

    mockMvc
        .perform(
            get("/api/classes/{classId}/assignments/latest", fixture.classId())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.runId", equalTo(runId)))
        .andExpect(jsonPath("$.satisfactionScore", equalTo(1.0)))
        .andExpect(jsonPath("$.firstChoiceCount", equalTo(2)))
        .andExpect(jsonPath("$.topThreeCount", equalTo(2)))
        .andExpect(jsonPath("$.worseThanThirdCount", equalTo(0)))
        .andExpect(jsonPath("$.unassignedStudentCount", equalTo(0)))
        .andExpect(jsonPath("$.results.length()", equalTo(2)))
        .andExpect(jsonPath("$.studentRankings.length()", equalTo(2)))
        .andExpect(jsonPath("$.studentRankings[0].rankings.length()", equalTo(2)))
        .andExpect(jsonPath("$.studentRankings[0].rankings[0].rank", equalTo(1)))
        .andExpect(jsonPath("$.studentRankings[0].rankings[1].rank", equalTo(2)))
        .andExpect(jsonPath("$.studentRankings[1].rankings.length()", equalTo(2)))
        .andExpect(jsonPath("$.studentRankings[1].rankings[0].rank", equalTo(1)))
        .andExpect(jsonPath("$.studentRankings[1].rankings[1].rank", equalTo(2)));

    mockMvc
        .perform(
            get("/api/classes/{classId}/assignments", fixture.classId())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.runs.length()", equalTo(1)))
        .andExpect(jsonPath("$.runs[0].runId", equalTo(runId)))
        .andExpect(jsonPath("$.runs[0].status", equalTo("COMPLETE")))
        .andExpect(jsonPath("$.runs[0].satisfactionScore", equalTo(1.0)))
        .andExpect(jsonPath("$.runs[0].firstChoiceCount", equalTo(2)))
        .andExpect(jsonPath("$.runs[0].topThreeCount", equalTo(2)))
        .andExpect(jsonPath("$.runs[0].worseThanThirdCount", equalTo(0)))
        .andExpect(jsonPath("$.runs[0].unassignedStudentCount", equalTo(0)));
  }

  @Test
  void assignmentGridsSupportPublicClassViewAndAuthenticatedTeacherSpreadsheet() throws Exception {
    String token = registerAndLoginTeacher();
    ClassFixture firstClass = createClassPeriod(token, "First Assignment Class");
    String firstSharedBookId = addBook(token, firstClass.classId(), "Shared Book", 2);
    String unusedBookId = addBook(token, firstClass.classId(), "Unused Book", 1);
    String alphaStudentId = joinClass(firstClass.joinCode(), "alpha-student");
    String betaStudentId = joinClass(firstClass.joinCode(), "beta-student");
    submitRankings(alphaStudentId, firstSharedBookId, unusedBookId);
    submitRankings(betaStudentId, firstSharedBookId, unusedBookId);

    ClassFixture secondClass =
        createRankedClassPeriod(token, "Second Assignment Class", "Shared Book", "Other Book");

    mockMvc
        .perform(
            post("/api/classes/{classId}/assign", firstClass.classId())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/classes/{classId}/assign", secondClass.classId())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/api/public/classes/{joinCode}/assignment-grid", firstClass.joinCode()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.classId", equalTo(firstClass.classId())))
        .andExpect(jsonPath("$.className", equalTo("First Assignment Class")))
        .andExpect(jsonPath("$.joinCode", equalTo(firstClass.joinCode())))
        .andExpect(jsonPath("$.assignmentRunId", not(blankOrNullString())))
        .andExpect(jsonPath("$.rows.length()", equalTo(2)))
        .andExpect(jsonPath("$.rows[0].bookTitle", equalTo("Shared Book")))
        .andExpect(jsonPath("$.rows[0].students[0]", equalTo("alpha-student")))
        .andExpect(jsonPath("$.rows[0].students[1]", equalTo("beta-student")))
        .andExpect(jsonPath("$.rows[1].bookTitle", equalTo("Unused Book")))
        .andExpect(jsonPath("$.rows[1].students.length()", equalTo(0)));

    mockMvc.perform(get("/api/teachers/me/assignment-grid")).andExpect(status().isForbidden());

    mockMvc
        .perform(
            get("/api/teachers/me/assignment-grid")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.columns.length()", equalTo(2)))
        .andExpect(jsonPath("$.columns[0].classId", equalTo(firstClass.classId())))
        .andExpect(jsonPath("$.columns[1].classId", equalTo(secondClass.classId())))
        .andExpect(jsonPath("$.rows[0].bookTitle", equalTo("Other Book")))
        .andExpect(jsonPath("$.rows[0].cells['%s']".formatted(firstClass.classId()), equalTo("")))
        .andExpect(
            jsonPath(
                "$.rows[0].cells['%s']".formatted(secondClass.classId()), equalTo("student-two")))
        .andExpect(jsonPath("$.rows[1].bookTitle", equalTo("Shared Book")))
        .andExpect(
            jsonPath(
                "$.rows[1].cells['%s']".formatted(firstClass.classId()), equalTo("alpha-student")))
        .andExpect(
            jsonPath(
                "$.rows[1].cells['%s']".formatted(secondClass.classId()), equalTo("student-one")))
        .andExpect(jsonPath("$.rows[2].bookTitle", equalTo("")))
        .andExpect(
            jsonPath(
                "$.rows[2].cells['%s']".formatted(firstClass.classId()), equalTo("beta-student")))
        .andExpect(jsonPath("$.rows[2].cells['%s']".formatted(secondClass.classId()), equalTo("")))
        .andExpect(jsonPath("$.rows[3].bookTitle", equalTo("Unused Book")))
        .andExpect(jsonPath("$.rows[3].cells['%s']".formatted(firstClass.classId()), equalTo("")))
        .andExpect(jsonPath("$.rows[3].cells['%s']".formatted(secondClass.classId()), equalTo("")));
  }

  @Test
  void teacherCanClearStudentDataForNewTerm() throws Exception {
    String token = registerAndLoginTeacher();
    ClassFixture fixture = createRankedClassPeriod(token);

    mockMvc
        .perform(
            post("/api/classes/{classId}/assign", fixture.classId())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk());

    mockMvc
        .perform(delete("/api/classes/{classId}/students", fixture.classId()))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            delete("/api/classes/{classId}/students", fixture.classId())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            get("/api/classes/{classId}", fixture.classId())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.books.length()", equalTo(2)))
        .andExpect(jsonPath("$.students.length()", equalTo(0)));

    mockMvc
        .perform(
            get("/api/classes/{classId}/assignments/latest", fixture.classId())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(get("/api/public/classes/{joinCode}/assignment-grid", fixture.joinCode()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.assignmentRunId").doesNotExist())
        .andExpect(jsonPath("$.rows.length()", equalTo(2)))
        .andExpect(jsonPath("$.rows[0].students.length()", equalTo(0)))
        .andExpect(jsonPath("$.rows[1].students.length()", equalTo(0)));
  }

  private ClassFixture createRankedClassPeriod(String token) throws Exception {
    return createRankedClassPeriod(token, "Assignment Class", "Book One", "Book Two");
  }

  private ClassFixture createRankedClassPeriod(
      String token, String className, String firstBookTitle, String secondBookTitle)
      throws Exception {
    ClassFixture fixture = createClassPeriod(token, className);

    String bookOneId = addBook(token, fixture.classId(), firstBookTitle, 1);
    String bookTwoId = addBook(token, fixture.classId(), secondBookTitle, 1);
    String studentOneId = joinClass(fixture.joinCode(), "student-one");
    String studentTwoId = joinClass(fixture.joinCode(), "student-two");

    submitRankings(studentOneId, bookOneId, bookTwoId);
    submitRankings(studentTwoId, bookTwoId, bookOneId);

    return fixture;
  }

  private ClassFixture createClassPeriod(String token, String className) throws Exception {
    MvcResult classResult =
        mockMvc
            .perform(
                post("/api/classes")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                {
                  "name": "%s"
                }
                """
                            .formatted(className)))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode classJson = objectMapper.readTree(classResult.getResponse().getContentAsString());
    String classId = classJson.get("classId").asText();
    String joinCode = classJson.get("joinCode").asText();

    return new ClassFixture(classId, joinCode);
  }

  private String registerAndLoginTeacher() throws Exception {
    String email = "assignment-teacher-%s@example.com".formatted(UUID.randomUUID());

    mockMvc
        .perform(
            post("/api/teachers/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                  "email": "%s",
                  "password": "password123"
                }
                """
                        .formatted(email)))
        .andExpect(status().isOk());

    MvcResult loginResult =
        mockMvc
            .perform(
                post("/api/teachers/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                {
                  "email": "%s",
                  "password": "password123"
                }
                """
                            .formatted(email)))
            .andExpect(status().isOk())
            .andReturn();

    return objectMapper
        .readTree(loginResult.getResponse().getContentAsString())
        .get("token")
        .asText();
  }

  private String addBook(String token, String classId, String title, int capacity)
      throws Exception {
    MvcResult bookResult =
        mockMvc
            .perform(
                post("/api/classes/{classId}/books", classId)
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                {
                  "title": "%s",
                  "capacity": %d
                }
                """
                            .formatted(title, capacity)))
            .andExpect(status().isOk())
            .andReturn();

    return objectMapper
        .readTree(bookResult.getResponse().getContentAsString())
        .get("bookId")
        .asText();
  }

  private String joinClass(String joinCode, String username) throws Exception {
    MvcResult joinResult =
        mockMvc
            .perform(
                post("/api/classes/join")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                {
                  "joinCode": "%s",
                  "username": "%s"
                }
                """
                            .formatted(joinCode, username)))
            .andExpect(status().isOk())
            .andReturn();

    return objectMapper
        .readTree(joinResult.getResponse().getContentAsString())
        .get("studentId")
        .asText();
  }

  private void submitRankings(String studentId, String firstBookId, String secondBookId)
      throws Exception {
    mockMvc
        .perform(
            post("/api/students/{studentId}/rankings", studentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                  "rankings": [
                    { "bookId": "%s", "rank": 1 },
                    { "bookId": "%s", "rank": 2 }
                  ]
                }
                """
                        .formatted(firstBookId, secondBookId)))
        .andExpect(status().isOk());
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }

  private record ClassFixture(String classId, String joinCode) {}
}
