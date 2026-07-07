package com.bookranker.auth.controller;

import com.bookranker.auth.dto.LoginRequest;
import com.bookranker.auth.dto.LoginResponse;
import com.bookranker.auth.dto.RegisterTeacherRequest;
import com.bookranker.auth.dto.RegisterTeacherResponse;
import com.bookranker.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teachers")
@Tag(name = "Teacher APIs", description = "APIs for teacher registration and authentication")
public class TeacherAuthController {

  private final AuthService authService;

  public TeacherAuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/register")
  @Operation(summary = "Create a teacher account")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Teacher account created successfully",
          content = @Content(schema = @Schema(implementation = RegisterTeacherResponse.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
      @ApiResponse(responseCode = "409", description = "Teacher email already exists", content = @Content)
  })
  public RegisterTeacherResponse register(@Valid @RequestBody RegisterTeacherRequest request) {
    return authService.register(request);
  }

  @PostMapping("/login")
  @Operation(summary = "Log in as a teacher")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Login successful",
          content = @Content(schema = @Schema(implementation = LoginResponse.class))),
      @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
      @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content)
  })
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }
}
