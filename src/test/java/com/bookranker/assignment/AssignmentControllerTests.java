package com.bookranker.assignment;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
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

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void assignmentRunRequiresAuthentication() throws Exception {
    mockMvc.perform(post("/api/classes/{classId}/assign", UUID.randomUUID()))
        .andExpect(status().isForbidden());
  }

  @Test
  void teacherCanRunAssignmentAndRetrieveResults() throws Exception {
    String token = registerAndLoginTeacher();
    ClassFixture fixture = createRankedClassPeriod(token);

    MvcResult runResult = mockMvc.perform(post("/api/classes/{classId}/assign", fixture.classId())
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

    String runId = objectMapper.readTree(runResult.getResponse().getContentAsString())
        .get("assignmentRunId")
        .asText();

    mockMvc.perform(get("/api/classes/{classId}/assignments/latest", fixture.classId())
            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.runId", equalTo(runId)))
        .andExpect(jsonPath("$.satisfactionScore", equalTo(1.0)))
        .andExpect(jsonPath("$.firstChoiceCount", equalTo(2)))
        .andExpect(jsonPath("$.topThreeCount", equalTo(2)))
        .andExpect(jsonPath("$.worseThanThirdCount", equalTo(0)))
        .andExpect(jsonPath("$.unassignedStudentCount", equalTo(0)))
        .andExpect(jsonPath("$.results.length()", equalTo(2)));

    mockMvc.perform(get("/api/classes/{classId}/assignments", fixture.classId())
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

  private ClassFixture createRankedClassPeriod(String token) throws Exception {
    MvcResult classResult = mockMvc.perform(post("/api/classes")
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Assignment Class"
                }
                """))
        .andExpect(status().isOk())
        .andReturn();

    JsonNode classJson = objectMapper.readTree(classResult.getResponse().getContentAsString());
    String classId = classJson.get("classId").asText();
    String joinCode = classJson.get("joinCode").asText();

    String bookOneId = addBook(token, classId, "Book One", 1);
    String bookTwoId = addBook(token, classId, "Book Two", 1);
    String studentOneId = joinClass(joinCode, "student-one");
    String studentTwoId = joinClass(joinCode, "student-two");

    submitRankings(studentOneId, bookOneId, bookTwoId);
    submitRankings(studentTwoId, bookTwoId, bookOneId);

    return new ClassFixture(classId);
  }

  private String registerAndLoginTeacher() throws Exception {
    String email = "assignment-teacher-%s@example.com".formatted(UUID.randomUUID());

    mockMvc.perform(post("/api/teachers/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "%s",
                  "password": "password123"
                }
                """.formatted(email)))
        .andExpect(status().isOk());

    MvcResult loginResult = mockMvc.perform(post("/api/teachers/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "%s",
                  "password": "password123"
                }
                """.formatted(email)))
        .andExpect(status().isOk())
        .andReturn();

    return objectMapper.readTree(loginResult.getResponse().getContentAsString())
        .get("token")
        .asText();
  }

  private String addBook(String token, String classId, String title, int capacity) throws Exception {
    MvcResult bookResult = mockMvc.perform(post("/api/classes/{classId}/books", classId)
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "title": "%s",
                  "capacity": %d
                }
                """.formatted(title, capacity)))
        .andExpect(status().isOk())
        .andReturn();

    return objectMapper.readTree(bookResult.getResponse().getContentAsString())
        .get("bookId")
        .asText();
  }

  private String joinClass(String joinCode, String username) throws Exception {
    MvcResult joinResult = mockMvc.perform(post("/api/classes/join")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "joinCode": "%s",
                  "username": "%s"
                }
                """.formatted(joinCode, username)))
        .andExpect(status().isOk())
        .andReturn();

    return objectMapper.readTree(joinResult.getResponse().getContentAsString())
        .get("studentId")
        .asText();
  }

  private void submitRankings(String studentId, String firstBookId, String secondBookId) throws Exception {
    mockMvc.perform(post("/api/students/{studentId}/rankings", studentId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "rankings": [
                    { "bookId": "%s", "rank": 1 },
                    { "bookId": "%s", "rank": 2 }
                  ]
                }
                """.formatted(firstBookId, secondBookId)))
        .andExpect(status().isOk());
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }

  private record ClassFixture(String classId) {
  }
}
