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
class ClassPeriodControllerTests extends ControllerTestSupport {

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

    submitRankings(studentOneId, bookOneId, bookTwoId);

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
}
