package com.bookranker.domain;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookranker.support.ControllerTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class RankingControllerIntegrationTests extends ControllerTestSupport {

  @Test
  void teacherCanManageClassPeriodAndStudentCanSubmitRankings() throws Exception {
    String token = registerAndLoginTeacher();
    MvcResult classResult = createClassPeriod(token, "English 12");
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

    String studentId = joinClass(joinCode, "student123", classId);

    mockMvc
        .perform(get("/api/students/{studentId}/status", studentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.submitted", equalTo(false)))
        .andExpect(jsonPath("$.rankCount", equalTo(0)))
        .andExpect(jsonPath("$.totalBooks", equalTo(2)))
        .andExpect(jsonPath("$.rankings.length()", equalTo(0)));

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
        .andExpect(jsonPath("$.totalBooks", equalTo(2)))
        .andExpect(jsonPath("$.rankings[0].bookId", equalTo(bookOneId)))
        .andExpect(jsonPath("$.rankings[0].rank", equalTo(1)))
        .andExpect(jsonPath("$.rankings[1].bookId", equalTo(bookTwoId)))
        .andExpect(jsonPath("$.rankings[1].rank", equalTo(2)));
  }

  @Test
  void rankingsMustMeetConfiguredMinimumRankingCount() throws Exception {
    String token = registerAndLoginTeacher();
    MvcResult classResult = createClassPeriod(token, "Incomplete Ranking Class");
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
            .andExpect(jsonPath("$.studentId", not(blankOrNullString())))
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
        .andExpect(jsonPath("$.minimumRankingCount", equalTo(1)))
        .andExpect(jsonPath("$.rankings[0].bookId", equalTo(bookOneId)))
        .andExpect(jsonPath("$.rankings[0].rank", equalTo(1)));

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
}
