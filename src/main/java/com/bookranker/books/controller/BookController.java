package com.bookranker.books.controller;

import com.bookranker.books.dto.BookResponse;
import com.bookranker.books.dto.BooksResponse;
import com.bookranker.books.dto.CreateBookRequest;
import com.bookranker.books.dto.CreateBookResponse;
import com.bookranker.books.dto.UpdateBookRequest;
import com.bookranker.books.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Book added successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid teacher JWT"),
        @ApiResponse(responseCode = "404", description = "Class period not found")
      })
  public CreateBookResponse addBook(
      @Parameter(description = "Class period ID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          String classId,
      @Valid @RequestBody CreateBookRequest request,
      Principal principal) {
    return bookService.addBook(classId, request, principal.getName());
  }

  @GetMapping
  @Operation(summary = "Get books in a class period")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Books returned successfully"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid teacher JWT"),
        @ApiResponse(responseCode = "404", description = "Class period not found")
      })
  public BooksResponse getBooks(
      @Parameter(description = "Class period ID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          String classId,
      Principal principal) {
    return bookService.getBooks(classId, principal.getName());
  }

  @PatchMapping("/{bookId}")
  @Operation(summary = "Update a book")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Book updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid teacher JWT"),
        @ApiResponse(responseCode = "403", description = "Class period belongs to another teacher"),
        @ApiResponse(responseCode = "404", description = "Class period or book not found")
      })
  public BookResponse updateBook(
      @Parameter(description = "Class period ID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          String classId,
      @Parameter(description = "Book ID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          String bookId,
      @Valid @RequestBody UpdateBookRequest request,
      Principal principal) {
    return bookService.updateBook(classId, bookId, request, principal.getName());
  }

  @DeleteMapping("/{bookId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete a book")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Book deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid teacher JWT"),
        @ApiResponse(responseCode = "403", description = "Class period belongs to another teacher"),
        @ApiResponse(responseCode = "404", description = "Class period or book not found")
      })
  public void deleteBook(
      @Parameter(description = "Class period ID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          String classId,
      @Parameter(description = "Book ID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          String bookId,
      Principal principal) {
    bookService.deleteBook(classId, bookId, principal.getName());
  }
}
