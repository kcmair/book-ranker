package com.bookranker.books.service;

import com.bookranker.books.dto.BookResponse;
import com.bookranker.books.model.Book;
import org.springframework.stereotype.Component;

@Component
public class BookResponseMapper {

  public BookResponse toResponse(Book book) {
    return new BookResponse(book.getId(), book.getTitle(), book.getCapacity());
  }
}
