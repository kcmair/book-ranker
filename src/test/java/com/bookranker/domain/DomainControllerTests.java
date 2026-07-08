package com.bookranker.domain;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class DomainControllerTests {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void classPeriodManagementRequiresAuthentication() throws Exception {
    mockMvc
        .perform(
            post("/api/classes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                  "name": "English 12"
                }
                """))
        .andExpect(status().isForbidden());

    mockMvc.perform(get("/api/classes")).andExpect(status().isForbidden());
  }

  @Test
  void teacherCanListOnlyOwnedClassPeriods() throws Exception {
    String teacherToken = registerAndLoginTeacher();
    String otherTeacherToken = registerAndLoginTeacher();

    createClassPeriod(teacherToken, "English 12");
    createClassPeriod(teacherToken, "Creative Writing");
    createClassPeriod(otherTeacherToken, "Other Teacher Class");

    mockMvc
        .perform(get("/api/classes").header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.classes.length()", equalTo(2)))
        .andExpect(jsonPath("$.classes[*].id", everyItem(not(blankOrNullString()))))
        .andExpect(
            jsonPath("$.classes[*].name", containsInAnyOrder("English 12", "Creative Writing")))
        .andExpect(jsonPath("$.classes[*].joinCode", everyItem(not(blankOrNullString()))));
  }

  @Test
  void teacherCanManageClassPeriodAndStudentCanSubmitRankings() throws Exception {
    String token = registerAndLoginTeacher();

    MvcResult classResult =
        mockMvc
            .perform(
                post("/api/classes")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                {
                  "name": "English 12"
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.classId", not(blankOrNullString())))
            .andExpect(jsonPath("$.joinCode", not(blankOrNullString())))
            .andReturn();

    JsonNode classJson = objectMapper.readTree(classResult.getResponse().getContentAsString());
    String classId = classJson.get("classId").asText();
    String joinCode = classJson.get("joinCode").asText();

    mockMvc
        .perform(
            get("/api/classes/{classId}", classId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", equalTo(classId)))
        .andExpect(jsonPath("$.name", equalTo("English 12")));

    String bookOneId = addBook(token, classId, "Book One", 2);
    String bookTwoId = addBook(token, classId, "Book Two", 1);

    mockMvc
        .perform(
            get("/api/classes/{classId}/books", classId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.books.length()", equalTo(2)));

    MvcResult joinResult =
        mockMvc
            .perform(
                post("/api/classes/join")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                {
                  "joinCode": "%s",
                  "username": "student123"
                }
                """
                            .formatted(joinCode)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.studentId", not(blankOrNullString())))
            .andExpect(jsonPath("$.classId", equalTo(classId)))
            .andReturn();

    String studentId =
        objectMapper
            .readTree(joinResult.getResponse().getContentAsString())
            .get("studentId")
            .asText();

    mockMvc
        .perform(get("/api/students/{studentId}/status", studentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.submitted", equalTo(false)))
        .andExpect(jsonPath("$.rankCount", equalTo(0)))
        .andExpect(jsonPath("$.totalBooks", equalTo(2)));

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
                        .formatted(bookOneId, bookTwoId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status", equalTo("submitted")));

    mockMvc
        .perform(get("/api/students/{studentId}/status", studentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.submitted", equalTo(true)))
        .andExpect(jsonPath("$.rankCount", equalTo(2)))
        .andExpect(jsonPath("$.totalBooks", equalTo(2)));
  }

  @Test
  void teacherCanEditAndRemoveClassPeriodsBooksAndStudents() throws Exception {
    String token = registerAndLoginTeacher();
    String otherTeacherToken = registerAndLoginTeacher();
    MvcResult classResult = createClassPeriod(token, "Original Class");
    JsonNode classJson = objectMapper.readTree(classResult.getResponse().getContentAsString());
    String classId = classJson.get("classId").asText();
    String joinCode = classJson.get("joinCode").asText();

    mockMvc
        .perform(
            patch("/api/classes/{classId}", classId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                  "name": "Updated Class"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", equalTo(classId)))
        .andExpect(jsonPath("$.name", equalTo("Updated Class")))
        .andExpect(jsonPath("$.joinCode", equalTo(joinCode)));

    mockMvc
        .perform(
            patch("/api/classes/{classId}", classId)
                .header(HttpHeaders.AUTHORIZATION, bearer(otherTeacherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                  "name": "Wrong Teacher Update"
                }
                """))
        .andExpect(status().isForbidden());

    String bookOneId = addBook(token, classId, "Book One", 2);
    String bookTwoId = addBook(token, classId, "Book Two", 1);

    mockMvc
        .perform(
            patch("/api/classes/{classId}/books/{bookId}", classId, bookOneId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                  "title": "Updated Book One",
                  "capacity": 3
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", equalTo(bookOneId)))
        .andExpect(jsonPath("$.title", equalTo("Updated Book One")))
        .andExpect(jsonPath("$.capacity", equalTo(3)));

    mockMvc
        .perform(
            patch("/api/classes/{classId}/books/{bookId}", classId, bookOneId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                  "title": "Invalid Book",
                  "capacity": 0
                }
                """))
        .andExpect(status().isBadRequest());

    String studentOneId = joinClass(joinCode, "student-one", classId);
    String studentTwoId = joinClass(joinCode, "student-two", classId);

    mockMvc
        .perform(
            post("/api/students/{studentId}/rankings", studentOneId)
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
                        .formatted(bookOneId, bookTwoId)))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            patch("/api/classes/{classId}/students/{studentId}", classId, studentOneId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                  "username": "student-renamed"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", equalTo(studentOneId)))
        .andExpect(jsonPath("$.username", equalTo("student-renamed")));

    mockMvc
        .perform(
            patch("/api/classes/{classId}/students/{studentId}", classId, studentOneId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                  "username": "student-two"
                }
                """))
        .andExpect(status().isConflict());

    mockMvc
        .perform(
            delete("/api/classes/{classId}/books/{bookId}", classId, bookTwoId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            get("/api/classes/{classId}/books", classId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.books.length()", equalTo(1)))
        .andExpect(jsonPath("$.books[0].id", equalTo(bookOneId)));

    mockMvc
        .perform(get("/api/students/{studentId}/status", studentOneId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rankCount", equalTo(1)))
        .andExpect(jsonPath("$.totalBooks", equalTo(1)));

    mockMvc
        .perform(
            delete("/api/classes/{classId}/students/{studentId}", classId, studentTwoId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/students/{studentId}/status", studentTwoId))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(
            delete("/api/classes/{classId}", classId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            get("/api/classes/{classId}", classId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isNotFound());
  }

  @Test
  void rankingsMustMeetConfiguredMinimumRankingCount() throws Exception {
    String token = registerAndLoginTeacher();
    MvcResult classResult =
        mockMvc
            .perform(
                post("/api/classes")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                {
                  "name": "Incomplete Ranking Class"
                }
                """))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode classJson = objectMapper.readTree(classResult.getResponse().getContentAsString());
    String classId = classJson.get("classId").asText();
    String joinCode = classJson.get("joinCode").asText();
    String bookOneId = addBook(token, classId, "Book One", 2);
    addBook(token, classId, "Book Two", 1);

    MvcResult joinResult =
        mockMvc
            .perform(
                post("/api/classes/join")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                {
                  "joinCode": "%s",
                  "username": "student456"
                }
                """
                            .formatted(joinCode)))
            .andExpect(status().isOk())
            .andReturn();

    String studentId =
        objectMapper
            .readTree(joinResult.getResponse().getContentAsString())
            .get("studentId")
            .asText();

    mockMvc
        .perform(
            post("/api/students/{studentId}/rankings", studentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                  "rankings": [
                    { "bookId": "%s", "rank": 1 }
                  ]
                }
                """
                        .formatted(bookOneId)))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            patch("/api/classes/{classId}", classId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                  "name": "Incomplete Ranking Class",
                  "minimumRankingCount": 1
                }
                """))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/classes/{classId}", classId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.minimumRankingCount", equalTo(1)))
        .andExpect(jsonPath("$.minimumRankingCountExplicit", equalTo(true)));

    mockMvc
        .perform(
            post("/api/students/{studentId}/rankings", studentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                  "rankings": [
                    { "bookId": "%s", "rank": 1 }
                  ]
                }
                """
                        .formatted(bookOneId)))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/api/students/{studentId}/status", studentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.submitted", equalTo(true)))
        .andExpect(jsonPath("$.rankCount", equalTo(1)))
        .andExpect(jsonPath("$.totalBooks", equalTo(2)))
        .andExpect(jsonPath("$.minimumRankingCount", equalTo(1)));

    mockMvc
        .perform(
            patch("/api/classes/{classId}", classId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {
                  "name": "Incomplete Ranking Class",
                  "minimumRankingCount": 3
                }
                """))
        .andExpect(status().isBadRequest());
  }

  private String registerAndLoginTeacher() throws Exception {
    String email = "teacher-%s@example.com".formatted(UUID.randomUUID());

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

  private MvcResult createClassPeriod(String token, String name) throws Exception {
    return mockMvc
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
                        .formatted(name)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.classId", not(blankOrNullString())))
        .andExpect(jsonPath("$.joinCode", not(blankOrNullString())))
        .andReturn();
  }

  private String joinClass(String joinCode, String username, String classId) throws Exception {
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
            .andExpect(jsonPath("$.studentId", not(blankOrNullString())))
            .andExpect(jsonPath("$.classId", equalTo(classId)))
            .andReturn();

    return objectMapper
        .readTree(joinResult.getResponse().getContentAsString())
        .get("studentId")
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
            .andExpect(jsonPath("$.bookId", not(blankOrNullString())))
            .andReturn();

    return objectMapper
        .readTree(bookResult.getResponse().getContentAsString())
        .get("bookId")
        .asText();
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }
}
