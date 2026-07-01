package com.bookranker.domain;

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
class DomainControllerTests {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void classPeriodManagementRequiresAuthentication() throws Exception {
    mockMvc.perform(post("/api/classes")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "English 12"
                }
                """))
        .andExpect(status().isForbidden());
  }

  @Test
  void teacherCanManageClassPeriodAndStudentCanSubmitRankings() throws Exception {
    String token = registerAndLoginTeacher();

    MvcResult classResult = mockMvc.perform(post("/api/classes")
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
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

    mockMvc.perform(get("/api/classes/{classId}", classId)
            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", equalTo(classId)))
        .andExpect(jsonPath("$.name", equalTo("English 12")));

    String bookOneId = addBook(token, classId, "Book One", 2);
    String bookTwoId = addBook(token, classId, "Book Two", 1);

    mockMvc.perform(get("/api/classes/{classId}/books", classId)
            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.books.length()", equalTo(2)));

    MvcResult joinResult = mockMvc.perform(post("/api/classes/join")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "joinCode": "%s",
                  "username": "student123"
                }
                """.formatted(joinCode)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.studentId", not(blankOrNullString())))
        .andExpect(jsonPath("$.classId", equalTo(classId)))
        .andReturn();

    String studentId = objectMapper.readTree(joinResult.getResponse().getContentAsString())
        .get("studentId")
        .asText();

    mockMvc.perform(get("/api/students/{studentId}/status", studentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.submitted", equalTo(false)))
        .andExpect(jsonPath("$.rankCount", equalTo(0)))
        .andExpect(jsonPath("$.totalBooks", equalTo(2)));

    mockMvc.perform(post("/api/students/{studentId}/rankings", studentId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "rankings": [
                    { "bookId": "%s", "rank": 1 },
                    { "bookId": "%s", "rank": 2 }
                  ]
                }
                """.formatted(bookOneId, bookTwoId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status", equalTo("submitted")));

    mockMvc.perform(get("/api/students/{studentId}/status", studentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.submitted", equalTo(true)))
        .andExpect(jsonPath("$.rankCount", equalTo(2)))
        .andExpect(jsonPath("$.totalBooks", equalTo(2)));
  }

  @Test
  void rankingsMustIncludeAllClassBooks() throws Exception {
    String token = registerAndLoginTeacher();
    MvcResult classResult = mockMvc.perform(post("/api/classes")
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
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

    MvcResult joinResult = mockMvc.perform(post("/api/classes/join")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "joinCode": "%s",
                  "username": "student456"
                }
                """.formatted(joinCode)))
        .andExpect(status().isOk())
        .andReturn();

    String studentId = objectMapper.readTree(joinResult.getResponse().getContentAsString())
        .get("studentId")
        .asText();

    mockMvc.perform(post("/api/students/{studentId}/rankings", studentId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "rankings": [
                    { "bookId": "%s", "rank": 1 }
                  ]
                }
                """.formatted(bookOneId)))
        .andExpect(status().isBadRequest());
  }

  private String registerAndLoginTeacher() throws Exception {
    String email = "teacher-%s@example.com".formatted(UUID.randomUUID());

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
        .andExpect(jsonPath("$.bookId", not(blankOrNullString())))
        .andReturn();

    return objectMapper.readTree(bookResult.getResponse().getContentAsString())
        .get("bookId")
        .asText();
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }
}
