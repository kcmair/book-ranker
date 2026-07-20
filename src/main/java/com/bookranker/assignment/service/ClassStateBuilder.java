package com.bookranker.assignment.service;

import com.bookranker.algorithm.ClassState;
import com.bookranker.books.model.Book;
import com.bookranker.books.repository.BookRepository;
import com.bookranker.classperiods.model.ClassPeriod;
import com.bookranker.classperiods.repository.ClassPeriodRepository;
import com.bookranker.classperiods.service.ClassPeriodPolicy;
import com.bookranker.rankings.model.Ranking;
import com.bookranker.rankings.repository.RankingRepository;
import com.bookranker.students.model.Student;
import com.bookranker.students.repository.StudentRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ClassStateBuilder {

  private final StudentRepository studentRepository;
  private final BookRepository bookRepository;
  private final ClassPeriodPolicy classPeriodPolicy;
  private final RankingRepository rankingRepository;
  private final ClassPeriodRepository classPeriodRepository;

  public ClassStateBuilder(
      StudentRepository studentRepository,
      BookRepository bookRepository,
      ClassPeriodPolicy classPeriodPolicy,
      RankingRepository rankingRepository,
      ClassPeriodRepository classPeriodRepository) {
    this.studentRepository = studentRepository;
    this.bookRepository = bookRepository;
    this.classPeriodPolicy = classPeriodPolicy;
    this.rankingRepository = rankingRepository;
    this.classPeriodRepository = classPeriodRepository;
  }

  public BuiltClassState build(String classPeriodId) {
    ClassPeriod classPeriod =
        classPeriodRepository
            .findById(classPeriodId)
            .orElseThrow(() -> new IllegalArgumentException("Class period not found"));
    List<Student> domainStudents = studentRepository.findByClassPeriodId(classPeriodId);
    List<Book> domainBooks = bookRepository.findByClassPeriodId(classPeriodId);
    List<Ranking> domainRankings = rankingRepository.findByStudentClassPeriodId(classPeriodId);

    Map<String, Student> domainStudentsById =
        domainStudents.stream().collect(Collectors.toMap(Student::getId, Function.identity()));
    Map<String, Book> domainBooksById =
        domainBooks.stream().collect(Collectors.toMap(Book::getId, Function.identity()));

    Map<String, ClassState.Student> algorithmStudentsById =
        domainStudents.stream()
            .collect(
                Collectors.toMap(
                    Student::getId, student -> new ClassState.Student(student.getId())));
    Map<String, ClassState.Book> algorithmBooksById =
        domainBooks.stream()
            .collect(Collectors.toMap(Book::getId, book -> new ClassState.Book(book.getId())));
    List<ClassState.Student> algorithmStudents =
        domainStudents.stream().map(student -> algorithmStudentsById.get(student.getId())).toList();
    List<ClassState.Book> algorithmBooks =
        domainBooks.stream().map(book -> algorithmBooksById.get(book.getId())).toList();

    Map<ClassState.Student, Map<ClassState.Book, Integer>> rankings = new HashMap<>();
    for (Ranking ranking : domainRankings) {
      ClassState.Student student = algorithmStudentsById.get(ranking.getStudent().getId());
      ClassState.Book book = algorithmBooksById.get(ranking.getBook().getId());
      if (student == null || book == null) {
        continue;
      }
      rankings.computeIfAbsent(student, ignored -> new HashMap<>()).put(book, ranking.getRank());
    }

    Map<ClassState.Book, Integer> capacities = new HashMap<>();
    for (Book book : domainBooks) {
      capacities.put(algorithmBooksById.get(book.getId()), book.getCapacity());
    }

    ClassState classState =
        new ClassState(
            algorithmStudents,
            algorithmBooks,
            rankings,
            capacities,
            classPeriodPolicy.effectiveMinimumRankingCount(classPeriod, domainBooks.size()));

    return new BuiltClassState(classState, domainStudentsById, domainBooksById);
  }

  public record BuiltClassState(
      ClassState classState,
      Map<String, Student> domainStudentsById,
      Map<String, Book> domainBooksById) {}
}
