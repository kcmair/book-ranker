package com.bookranker.repository;

import com.bookranker.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, String> {
  List<Book> findByClassEntityId(String classId);
}
