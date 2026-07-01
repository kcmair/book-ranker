package com.bookranker.auth.controller;

import com.bookranker.auth.dto.LoginRequest;
import com.bookranker.auth.dto.LoginResponse;
import com.bookranker.auth.dto.RegisterTeacherRequest;
import com.bookranker.auth.dto.RegisterTeacherResponse;
import com.bookranker.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teachers")
public class TeacherAuthController {

  private final AuthService authService;

  public TeacherAuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/register")
  public RegisterTeacherResponse register(@Valid @RequestBody RegisterTeacherRequest request) {
    return authService.register(request);
  }

  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }
}
