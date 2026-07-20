package com.bookranker.support;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

public abstract class ControllerTestSupport {

  @Autowired protected MockMvc mockMvc;

  @Autowired protected ObjectMapper objectMapper;

  protected String registerAndLoginTeacher() throws Exception {
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

  protected MvcResult createClassPeriod(String token, String name) throws Exception {
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

  protected ClassFixture createClassFixture(String token, String className) throws Exception {
    MvcResult classResult = createClassPeriod(token, className);
    var classJson = objectMapper.readTree(classResult.getResponse().getContentAsString());
    return new ClassFixture(classJson.get("classId").asText(), classJson.get("joinCode").asText());
  }

  protected String addBook(String token, String classId, String title, int capacity)
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

  protected String joinClass(String joinCode, String username, String classId) throws Exception {
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

  protected void submitRankings(String studentId, String firstBookId, String secondBookId)
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

  protected String bearer(String token) {
    return "Bearer " + token;
  }

  protected record ClassFixture(String classId, String joinCode) {}
}
