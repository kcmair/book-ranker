package com.bookranker.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookranker.auth.dto.LoginRequest;
import com.bookranker.auth.dto.RegisterTeacherRequest;
import com.bookranker.auth.model.Teacher;
import com.bookranker.auth.repository.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
class AuthServiceTests {

  @Autowired private AuthService authService;

  @Autowired private TeacherRepository teacherRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private JwtService jwtService;

  @Test
  void registerStoresHashedPassword() {
    var response =
        authService.register(new RegisterTeacherRequest("teacher@example.com", "password123"));

    Teacher teacher = teacherRepository.findById(response.teacherId()).orElseThrow();
    assertThat(teacher.getEmail()).isEqualTo("teacher@example.com");
    assertThat(teacher.getPasswordHash()).isNotEqualTo("password123");
    assertThat(passwordEncoder.matches("password123", teacher.getPasswordHash())).isTrue();
  }

  @Test
  void loginReturnsTokenForValidCredentials() {
    authService.register(new RegisterTeacherRequest("login@example.com", "password123"));

    var response = authService.login(new LoginRequest("login@example.com", "password123"));

    assertThat(response.token()).isNotBlank();
    assertThat(jwtService.getSubject(response.token())).isEqualTo("login@example.com");
  }

  @Test
  void loginRejectsInvalidCredentials() {
    authService.register(new RegisterTeacherRequest("reject@example.com", "password123"));

    assertThatThrownBy(
            () -> authService.login(new LoginRequest("reject@example.com", "wrong-password")))
        .isInstanceOf(ResponseStatusException.class);
  }
}
