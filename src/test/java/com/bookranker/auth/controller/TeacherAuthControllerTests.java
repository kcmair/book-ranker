package com.bookranker.auth.controller;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TeacherAuthControllerTests {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void registerReturnsTeacherId() throws Exception {
    mockMvc.perform(post("/api/teachers/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "controller-register@example.com",
                  "password": "password123"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.teacherId", not(blankOrNullString())));
  }

  @Test
  void loginReturnsJwtToken() throws Exception {
    mockMvc.perform(post("/api/teachers/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "controller-login@example.com",
                  "password": "password123"
                }
                """))
        .andExpect(status().isOk());

    mockMvc.perform(post("/api/teachers/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "controller-login@example.com",
                  "password": "password123"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token", not(blankOrNullString())));
  }

  @Test
  void corsPreflightAllowsLocalFrontend() throws Exception {
    mockMvc.perform(options("/api/teachers/register")
            .header("Origin", "http://localhost:5173")
            .header("Access-Control-Request-Method", "POST")
            .header("Access-Control-Request-Headers", "content-type,authorization"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
        .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS"))
        .andExpect(header().string("Access-Control-Allow-Headers", "content-type, authorization"));
  }
}
