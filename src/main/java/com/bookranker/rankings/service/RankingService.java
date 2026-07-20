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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RankingService {

  private final RankingRepository rankingRepository;
  private final BookRepository bookRepository;
  private final StudentService studentService;
  private final ClassPeriodService classPeriodService;
  private final RankingSubmissionValidator rankingSubmissionValidator;

  public RankingService(
      RankingRepository rankingRepository,
      BookRepository bookRepository,
      StudentService studentService,
      ClassPeriodService classPeriodService,
      RankingSubmissionValidator rankingSubmissionValidator) {
    this.rankingRepository = rankingRepository;
    this.bookRepository = bookRepository;
    this.studentService = studentService;
    this.classPeriodService = classPeriodService;
    this.rankingSubmissionValidator = rankingSubmissionValidator;
  }

  @Transactional
  public SubmitRankingsResponse submitRankings(String studentId, SubmitRankingsRequest request) {
    Student student = studentService.findStudent(studentId);
    ClassPeriod classPeriod = student.getClassPeriod();
    String classPeriodId = classPeriod.getId();
    Map<String, Book> booksById =
        bookRepository.findByClassPeriodId(classPeriodId).stream()
            .collect(Collectors.toMap(Book::getId, Function.identity()));
    int minimumRankingCount =
        classPeriodService.effectiveMinimumRankingCount(classPeriod, booksById.size());

    rankingSubmissionValidator.validate(request, booksById, minimumRankingCount);

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
}
