package com.bookranker.books.repository;

import com.bookranker.books.model.Book;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, String> {

  List<Book> findByClassPeriodId(String classPeriodId);

  Optional<Book> findByIdAndClassPeriodId(String id, String classPeriodId);

  long countByClassPeriodId(String classPeriodId);
}
