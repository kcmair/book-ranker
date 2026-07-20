package com.bookranker.books.service;

import com.bookranker.books.dto.BookResponse;
import com.bookranker.books.dto.BooksResponse;
import com.bookranker.books.dto.CreateBookRequest;
import com.bookranker.books.dto.CreateBookResponse;
import com.bookranker.books.dto.UpdateBookRequest;
import com.bookranker.books.model.Book;
import com.bookranker.books.repository.BookRepository;
import com.bookranker.classperiods.model.ClassPeriod;
import com.bookranker.classperiods.service.ClassPeriodService;
import com.bookranker.rankings.repository.RankingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookService {

  private final BookRepository bookRepository;
  private final BookResponseMapper bookResponseMapper;
  private final ClassPeriodService classPeriodService;
  private final RankingRepository rankingRepository;

  public BookService(
      BookRepository bookRepository,
      BookResponseMapper bookResponseMapper,
      ClassPeriodService classPeriodService,
      RankingRepository rankingRepository) {
    this.bookRepository = bookRepository;
    this.bookResponseMapper = bookResponseMapper;
    this.classPeriodService = classPeriodService;
    this.rankingRepository = rankingRepository;
  }

  @Transactional
  public CreateBookResponse addBook(
      String classPeriodId, CreateBookRequest request, String teacherEmail) {
    if (request.capacity() < 1) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Book capacity must be greater than zero");
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
    ClassPeriod classPeriod = classPeriodService.findOwnedClassPeriod(classPeriodId, teacherEmail);
    return new BooksResponse(
        classPeriod.getName(),
        classPeriodService.effectiveMinimumRankingCount(classPeriod),
        bookRepository.findByClassPeriodId(classPeriodId).stream()
            .map(bookResponseMapper::toResponse)
            .toList());
  }

  @Transactional
  public BookResponse updateBook(
      String classPeriodId, String bookId, UpdateBookRequest request, String teacherEmail) {
    Book book = findOwnedBook(classPeriodId, bookId, teacherEmail);
    book.setTitle(request.title());
    book.setCapacity(request.capacity());
    return bookResponseMapper.toResponse(book);
  }

  @Transactional
  public void deleteBook(String classPeriodId, String bookId, String teacherEmail) {
    Book book = findOwnedBook(classPeriodId, bookId, teacherEmail);
    rankingRepository.deleteByBookId(book.getId());
    bookRepository.delete(book);
  }

  private Book findOwnedBook(String classPeriodId, String bookId, String teacherEmail) {
    classPeriodService.findOwnedClassPeriod(classPeriodId, teacherEmail);
    return bookRepository
        .findByIdAndClassPeriodId(bookId, classPeriodId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
  }
}
