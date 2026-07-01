package com.bookranker.books.controller;

import com.bookranker.books.dto.BooksResponse;
import com.bookranker.books.dto.CreateBookRequest;
import com.bookranker.books.dto.CreateBookResponse;
import com.bookranker.books.service.BookService;
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
public class BookController {

  private final BookService bookService;

  public BookController(BookService bookService) {
    this.bookService = bookService;
  }

  @PostMapping
  public CreateBookResponse addBook(
      @PathVariable String classId,
      @Valid @RequestBody CreateBookRequest request,
      Principal principal
  ) {
    return bookService.addBook(classId, request, principal.getName());
  }

  @GetMapping
  public BooksResponse getBooks(
      @PathVariable String classId,
      Principal principal
  ) {
    return bookService.getBooks(classId, principal.getName());
  }
}
