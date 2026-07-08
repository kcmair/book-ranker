package com.bookranker.auth.service;

import com.bookranker.auth.dto.LoginRequest;
import com.bookranker.auth.dto.LoginResponse;
import com.bookranker.auth.dto.RegisterTeacherRequest;
import com.bookranker.auth.dto.RegisterTeacherResponse;
import com.bookranker.auth.model.Teacher;
import com.bookranker.auth.repository.TeacherRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

  private final TeacherRepository teacherRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthService(
      TeacherRepository teacherRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
    this.teacherRepository = teacherRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  public RegisterTeacherResponse register(RegisterTeacherRequest request) {
    String email = normalizeEmail(request.email());
    if (teacherRepository.existsByEmail(email)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Teacher email already exists");
    }

    Teacher teacher = new Teacher();
    teacher.setEmail(email);
    teacher.setPasswordHash(passwordEncoder.encode(request.password()));

    Teacher saved = teacherRepository.save(teacher);
    return new RegisterTeacherResponse(saved.getId());
  }

  public LoginResponse login(LoginRequest request) {
    String email = normalizeEmail(request.email());
    Teacher teacher =
        teacherRepository
            .findByEmail(email)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

    if (!passwordEncoder.matches(request.password(), teacher.getPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    return new LoginResponse(jwtService.generateToken(teacher.getEmail()));
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase();
  }
}
