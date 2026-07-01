package com.bookranker.rankings.service;

import com.bookranker.books.model.Book;
import com.bookranker.books.repository.BookRepository;
import com.bookranker.rankings.dto.RankingItemRequest;
import com.bookranker.rankings.dto.SubmitRankingsRequest;
import com.bookranker.rankings.dto.SubmitRankingsResponse;
import com.bookranker.rankings.model.Ranking;
import com.bookranker.rankings.repository.RankingRepository;
import com.bookranker.students.model.Student;
import com.bookranker.students.service.StudentService;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RankingService {

  private final RankingRepository rankingRepository;
  private final BookRepository bookRepository;
  private final StudentService studentService;

  public RankingService(
      RankingRepository rankingRepository,
      BookRepository bookRepository,
      StudentService studentService
  ) {
    this.rankingRepository = rankingRepository;
    this.bookRepository = bookRepository;
    this.studentService = studentService;
  }

  @Transactional
  public SubmitRankingsResponse submitRankings(String studentId, SubmitRankingsRequest request) {
    Student student = studentService.findStudent(studentId);
    String classPeriodId = student.getClassPeriod().getId();
    Map<String, Book> booksById = bookRepository.findByClassPeriodId(classPeriodId).stream()
        .collect(Collectors.toMap(Book::getId, Function.identity()));

    validateRankings(request, booksById);

    rankingRepository.deleteByStudentId(studentId);
    Instant submittedAt = Instant.now();

    for (RankingItemRequest item : request.rankings()) {
      Ranking ranking = new Ranking();
      ranking.setStudent(student);
      ranking.setBook(booksById.get(item.bookId()));
      ranking.setRank(item.rank());
      ranking.setSubmittedAt(submittedAt);
      rankingRepository.save(ranking);
    }

    return new SubmitRankingsResponse("submitted");
  }

  private void validateRankings(SubmitRankingsRequest request, Map<String, Book> booksById) {
    if (request.rankings().size() != booksById.size()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rankings must include all books in the class");
    }

    Set<String> bookIds = new HashSet<>();
    Set<Integer> ranks = new HashSet<>();

    for (RankingItemRequest item : request.rankings()) {
      if (!booksById.containsKey(item.bookId())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ranking contains a book outside the class");
      }
      if (!bookIds.add(item.bookId())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rankings cannot contain duplicate books");
      }
      if (!ranks.add(item.rank())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rankings cannot contain duplicate ranks");
      }
    }

    for (int expectedRank = 1; expectedRank <= booksById.size(); expectedRank++) {
      if (!ranks.contains(expectedRank)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ranks must be contiguous from 1 to number of books");
      }
    }
  }
}
