package com.bookranker.assignment;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookranker.support.ControllerTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AssignmentControllerTests extends ControllerTestSupport {

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
    ClassFixture firstClass = createClassFixture(token, "First Assignment Class");
    String firstSharedBookId = addBook(token, firstClass.classId(), "Shared Book", 2);
    String unusedBookId = addBook(token, firstClass.classId(), "Unused Book", 1);
    String alphaStudentId = joinClass(firstClass.joinCode(), "alpha-student", firstClass.classId());
    String betaStudentId = joinClass(firstClass.joinCode(), "beta-student", firstClass.classId());
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
    ClassFixture fixture = createClassFixture(token, className);

    String bookOneId = addBook(token, fixture.classId(), firstBookTitle, 1);
    String bookTwoId = addBook(token, fixture.classId(), secondBookTitle, 1);
    String studentOneId = joinClass(fixture.joinCode(), "student-one", fixture.classId());
    String studentTwoId = joinClass(fixture.joinCode(), "student-two", fixture.classId());

    submitRankings(studentOneId, bookOneId, bookTwoId);
    submitRankings(studentTwoId, bookTwoId, bookOneId);

    return fixture;
  }
}
