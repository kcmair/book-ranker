package com.bookranker.assignment;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookranker.assignment.model.AssignmentRun;
import com.bookranker.assignment.model.AssignmentRunStatus;
import com.bookranker.assignment.repository.AssignmentRunRepository;
import com.bookranker.classperiods.repository.ClassPeriodRepository;
import com.bookranker.support.ControllerTestSupport;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AssignmentControllerTests extends ControllerTestSupport {

  @Autowired private AssignmentRunRepository assignmentRunRepository;

  @Autowired private ClassPeriodRepository classPeriodRepository;

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

    AssignmentRun pendingRun = new AssignmentRun();
    pendingRun.setClassPeriod(classPeriodRepository.findById(fixture.classId()).orElseThrow());
    pendingRun.setStatus(AssignmentRunStatus.PENDING);
    pendingRun.setCreatedAt(Instant.now().plusSeconds(1));
    assignmentRunRepository.save(pendingRun);

    mockMvc
        .perform(
            get("/api/classes/{classId}/assignments/latest", fixture.classId())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.runId", equalTo(runId)));

    mockMvc
        .perform(
            get("/api/classes/{classId}/assignments", fixture.classId())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.runs.length()", equalTo(2)))
        .andExpect(jsonPath("$.runs[0].status", equalTo("PENDING")))
        .andExpect(jsonPath("$.runs[1].runId", equalTo(runId)))
        .andExpect(jsonPath("$.runs[1].status", equalTo("COMPLETE")))
        .andExpect(jsonPath("$.runs[1].satisfactionScore", equalTo(1.0)))
        .andExpect(jsonPath("$.runs[1].firstChoiceCount", equalTo(2)))
        .andExpect(jsonPath("$.runs[1].topThreeCount", equalTo(2)))
        .andExpect(jsonPath("$.runs[1].worseThanThirdCount", equalTo(0)))
        .andExpect(jsonPath("$.runs[1].unassignedStudentCount", equalTo(0)));
  }

  @Test
  void teacherCanDeleteAssignmentRunsAndResetPublicAssignmentView() throws Exception {
    String token = registerAndLoginTeacher();
    ClassFixture fixture = createRankedClassPeriod(token);

    String firstRunId =
        objectMapper
            .readTree(
                mockMvc
                    .perform(
                        post("/api/classes/{classId}/assign", fixture.classId())
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .get("assignmentRunId")
            .asText();
    String secondRunId =
        objectMapper
            .readTree(
                mockMvc
                    .perform(
                        post("/api/classes/{classId}/assign", fixture.classId())
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .get("assignmentRunId")
            .asText();

    mockMvc
        .perform(
            get("/api/classes/{classId}/assignments/latest", fixture.classId())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.runId", equalTo(secondRunId)));

    mockMvc
        .perform(
            delete("/api/classes/{classId}/assignments/{runId}", fixture.classId(), secondRunId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            get("/api/classes/{classId}/assignments/latest", fixture.classId())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.runId", equalTo(firstRunId)));

    mockMvc
        .perform(get("/api/public/classes/{joinCode}/assignment-grid", fixture.joinCode()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.assignmentRunId", equalTo(firstRunId)));

    mockMvc
        .perform(
            delete("/api/classes/{classId}/assignments/{runId}", fixture.classId(), firstRunId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            get("/api/classes/{classId}/assignments/latest", fixture.classId())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(get("/api/public/classes/{joinCode}/assignment-grid", fixture.joinCode()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.assignmentRunId").doesNotExist());
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
  void teacherCanManuallyReassignStudentBeyondLatestAssignmentCapacity() throws Exception {
    String token = registerAndLoginTeacher();
    ClassFixture fixture = createClassFixture(token, "Manual Reassignment Class");
    String firstBookId = addBook(token, fixture.classId(), "First Book", 2);
    String secondBookId = addBook(token, fixture.classId(), "Second Book", 1);
    String firstStudentId = joinClass(fixture.joinCode(), "first-student", fixture.classId());
    String secondStudentId = joinClass(fixture.joinCode(), "second-student", fixture.classId());
    submitRankings(firstStudentId, firstBookId, secondBookId);
    submitRankings(secondStudentId, firstBookId, secondBookId);

    mockMvc
        .perform(
            post("/api/classes/{classId}/assign", fixture.classId())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstChoiceCount", equalTo(2)));

    mockMvc
        .perform(
            patch(
                    "/api/classes/{classId}/assignments/latest/students/{studentId}",
                    fixture.classId(),
                    firstStudentId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "bookId": "%s"
                    }
                    """
                        .formatted(secondBookId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.results.length()", equalTo(2)))
        .andExpect(jsonPath("$.firstChoiceCount", equalTo(1)))
        .andExpect(jsonPath("$.topThreeCount", equalTo(2)))
        .andExpect(jsonPath("$.totalCost", equalTo(1)))
        .andExpect(jsonPath("$.unassignedStudentCount", equalTo(0)));

    mockMvc
        .perform(
            get("/api/classes/{classId}/assignments/latest", fixture.classId())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstChoiceCount", equalTo(1)))
        .andExpect(jsonPath("$.totalCost", equalTo(1)));

    mockMvc
        .perform(
            patch(
                    "/api/classes/{classId}/assignments/latest/students/{studentId}",
                    fixture.classId(),
                    secondStudentId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "bookId": "%s"
                    }
                    """
                        .formatted(secondBookId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.results.length()", equalTo(2)))
        .andExpect(jsonPath("$.firstChoiceCount", equalTo(0)))
        .andExpect(jsonPath("$.topThreeCount", equalTo(2)))
        .andExpect(jsonPath("$.totalCost", equalTo(2)))
        .andExpect(jsonPath("$.unassignedStudentCount", equalTo(0)));
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
        .andExpect(jsonPath("$.joinCode", not(equalTo(fixture.joinCode()))))
        .andExpect(jsonPath("$.books.length()", equalTo(2)))
        .andExpect(jsonPath("$.students.length()", equalTo(0)));

    mockMvc
        .perform(
            get("/api/classes/{classId}/assignments/latest", fixture.classId())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(get("/api/public/classes/{joinCode}/assignment-grid", fixture.joinCode()))
        .andExpect(status().isNotFound());

    String newJoinCode =
        objectMapper
            .readTree(
                mockMvc
                    .perform(
                        get("/api/classes/{classId}", fixture.classId())
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .get("joinCode")
            .asText();

    mockMvc
        .perform(get("/api/public/classes/{joinCode}/assignment-grid", newJoinCode))
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
