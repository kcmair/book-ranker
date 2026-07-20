package com.bookranker.rankings.service;

import com.bookranker.books.model.Book;
import com.bookranker.rankings.dto.RankingItemRequest;
import com.bookranker.rankings.dto.SubmitRankingsRequest;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class RankingSubmissionValidator {

  public void validate(
      SubmitRankingsRequest request, Map<String, Book> booksById, int minimumRankingCount) {
    if (request.rankings().size() < minimumRankingCount) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Rankings must include at least the required minimum number of books");
    }
    if (request.rankings().size() > booksById.size()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Rankings cannot exceed number of books in the class");
    }

    Set<String> bookIds = new HashSet<>();
    Set<Integer> ranks = new HashSet<>();

    for (RankingItemRequest item : request.rankings()) {
      if (!booksById.containsKey(item.bookId())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Ranking contains a book outside the class");
      }
      if (!bookIds.add(item.bookId())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Rankings cannot contain duplicate books");
      }
      if (!ranks.add(item.rank())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Rankings cannot contain duplicate ranks");
      }
    }

    for (int expectedRank = 1; expectedRank <= request.rankings().size(); expectedRank++) {
      if (!ranks.contains(expectedRank)) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Ranks must be contiguous from 1 to submitted ranking count");
      }
    }
  }
}
