package com.bookranker.rankings.service;

import com.bookranker.books.model.Book;
import com.bookranker.books.repository.BookRepository;
import com.bookranker.classperiods.model.ClassPeriod;
import com.bookranker.classperiods.service.ClassPeriodService;
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
  private final ClassPeriodService classPeriodService;

  public RankingService(
      RankingRepository rankingRepository,
      BookRepository bookRepository,
      StudentService studentService,
      ClassPeriodService classPeriodService
  ) {
    this.rankingRepository = rankingRepository;
    this.bookRepository = bookRepository;
    this.studentService = studentService;
    this.classPeriodService = classPeriodService;
  }

  @Transactional
  public SubmitRankingsResponse submitRankings(String studentId, SubmitRankingsRequest request) {
    Student student = studentService.findStudent(studentId);
    ClassPeriod classPeriod = student.getClassPeriod();
    String classPeriodId = classPeriod.getId();
    Map<String, Book> booksById = bookRepository.findByClassPeriodId(classPeriodId).stream()
        .collect(Collectors.toMap(Book::getId, Function.identity()));
    int minimumRankingCount = classPeriodService.effectiveMinimumRankingCount(classPeriod, booksById.size());

    validateRankings(request, booksById, minimumRankingCount);

    rankingRepository.deleteByStudentId(studentId);
    rankingRepository.flush();
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

  private void validateRankings(
      SubmitRankingsRequest request,
      Map<String, Book> booksById,
      int minimumRankingCount
  ) {
    if (request.rankings().size() < minimumRankingCount) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Rankings must include at least the required minimum number of books"
      );
    }
    if (request.rankings().size() > booksById.size()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rankings cannot exceed number of books in the class");
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

    for (int expectedRank = 1; expectedRank <= request.rankings().size(); expectedRank++) {
      if (!ranks.contains(expectedRank)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ranks must be contiguous from 1 to submitted ranking count");
      }
    }
  }
}
