package com.bookranker.books.service;

import com.bookranker.books.dto.BookResponse;
import com.bookranker.books.dto.BooksResponse;
import com.bookranker.books.dto.CreateBookRequest;
import com.bookranker.books.dto.CreateBookResponse;
import com.bookranker.books.model.Book;
import com.bookranker.books.repository.BookRepository;
import com.bookranker.classperiods.model.ClassPeriod;
import com.bookranker.classperiods.service.ClassPeriodService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookService {

  private final BookRepository bookRepository;
  private final ClassPeriodService classPeriodService;

  public BookService(BookRepository bookRepository, ClassPeriodService classPeriodService) {
    this.bookRepository = bookRepository;
    this.classPeriodService = classPeriodService;
  }

  @Transactional
  public CreateBookResponse addBook(String classPeriodId, CreateBookRequest request, String teacherEmail) {
    if (request.capacity() < 1) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Book capacity must be greater than zero");
    }

    ClassPeriod classPeriod = classPeriodService.findOwnedClassPeriod(classPeriodId, teacherEmail);
    Book book = new Book();
    book.setClassPeriod(classPeriod);
    book.setTitle(request.title());
    book.setCapacity(request.capacity());

    Book saved = bookRepository.save(book);
    return new CreateBookResponse(saved.getId());
  }

  @Transactional(readOnly = true)
  public BooksResponse getBooks(String classPeriodId, String teacherEmail) {
    classPeriodService.findOwnedClassPeriod(classPeriodId, teacherEmail);
    return new BooksResponse(bookRepository.findByClassPeriodId(classPeriodId).stream()
        .map(book -> new BookResponse(book.getId(), book.getTitle(), book.getCapacity()))
        .toList());
  }
}
