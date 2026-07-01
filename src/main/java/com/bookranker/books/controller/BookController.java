package com.bookranker.books.controller;

import com.bookranker.books.dto.BooksResponse;
import com.bookranker.books.dto.CreateBookRequest;
import com.bookranker.books.dto.CreateBookResponse;
import com.bookranker.books.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/classes/{classId}/books")
@Tag(name = "Book APIs", description = "APIs for managing books in teacher-owned class periods")
@SecurityRequirement(name = "bearerAuth")
public class BookController {

  private final BookService bookService;

  public BookController(BookService bookService) {
    this.bookService = bookService;
  }

  @PostMapping
  @Operation(summary = "Add a book to a class period")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Book added successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid request"),
      @ApiResponse(responseCode = "401", description = "Missing or invalid teacher JWT"),
      @ApiResponse(responseCode = "404", description = "Class period not found")
  })
  public CreateBookResponse addBook(
      @Parameter(description = "Class period ID", example = "550e8400-e29b-41d4-a716-446655440000")
      @PathVariable String classId,
      @Valid @RequestBody CreateBookRequest request,
      Principal principal
  ) {
    return bookService.addBook(classId, request, principal.getName());
  }

  @GetMapping
  @Operation(summary = "Get books in a class period")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Books returned successfully"),
      @ApiResponse(responseCode = "401", description = "Missing or invalid teacher JWT"),
      @ApiResponse(responseCode = "404", description = "Class period not found")
  })
  public BooksResponse getBooks(
      @Parameter(description = "Class period ID", example = "550e8400-e29b-41d4-a716-446655440000")
      @PathVariable String classId,
      Principal principal
  ) {
    return bookService.getBooks(classId, principal.getName());
  }
}
