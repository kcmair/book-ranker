package com.bookranker.books.repository;

import com.bookranker.books.model.Book;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, String> {

  List<Book> findByClassPeriodId(String classPeriodId);

  long countByClassPeriodId(String classPeriodId);
}
