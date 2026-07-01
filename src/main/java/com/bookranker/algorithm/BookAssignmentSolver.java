package com.bookranker.algorithm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

public class BookAssignmentSolver {

  public AssignmentResult solve(ClassState classState) {
    Objects.requireNonNull(classState, "classState");

    List<ClassState.Student> eligibleStudents = eligibleStudents(classState);
    List<ClassState.Book> books = classState.books();

    int source = 0;
    int studentOffset = 1;
    int bookOffset = studentOffset + eligibleStudents.size();
    int sink = bookOffset + books.size();
    MinCostFlowGraph graph = new MinCostFlowGraph(sink + 1);

    for (int studentIndex = 0; studentIndex < eligibleStudents.size(); studentIndex++) {
      graph.addEdge(source, studentOffset + studentIndex, 1, 0);
    }

    List<StudentBookEdge> studentBookEdges = new ArrayList<>();
    for (int studentIndex = 0; studentIndex < eligibleStudents.size(); studentIndex++) {
      ClassState.Student student = eligibleStudents.get(studentIndex);
      Map<ClassState.Book, Integer> studentRankings = classState.rankings().get(student);

      for (int bookIndex = 0; bookIndex < books.size(); bookIndex++) {
        ClassState.Book book = books.get(bookIndex);
        int rank = studentRankings.get(book);
        int cost = RankingCost.fromRank(rank);
        MinCostFlowGraph.Edge edge = graph.addEdge(
            studentOffset + studentIndex,
            bookOffset + bookIndex,
            1,
            cost
        );
        studentBookEdges.add(new StudentBookEdge(student, book, rank, edge));
      }
    }

    for (int bookIndex = 0; bookIndex < books.size(); bookIndex++) {
      ClassState.Book book = books.get(bookIndex);
      int capacity = classState.capacities().getOrDefault(book, 0);
      graph.addEdge(bookOffset + bookIndex, sink, capacity, 0);
    }

    MinCostFlowGraph.FlowResult flowResult = graph.minCostMaxFlow(source, sink, eligibleStudents.size());

    Map<ClassState.Student, ClassState.Book> assignments = new LinkedHashMap<>();
    Map<ClassState.Student, Integer> assignedRanks = new HashMap<>();
    for (StudentBookEdge candidate : studentBookEdges) {
      if (candidate.edge().flow() > 0) {
        assignments.put(candidate.student(), candidate.book());
        assignedRanks.put(candidate.student(), candidate.rank());
      }
    }

    Set<ClassState.Student> unassigned = new LinkedHashSet<>(classState.students());
    unassigned.removeAll(assignments.keySet());

    return new AssignmentResult(
        assignments,
        unassigned,
        flowResult.cost(),
        satisfactionScore(flowResult.cost(), assignments.size(), books.size()),
        satisfactionDistribution(assignedRanks)
    );
  }

  private List<ClassState.Student> eligibleStudents(ClassState classState) {
    List<ClassState.Student> eligible = new ArrayList<>();
    for (ClassState.Student student : classState.students()) {
      Map<ClassState.Book, Integer> studentRankings = classState.rankings().get(student);
      if (studentRankings == null || !studentRankings.keySet().containsAll(classState.books())) {
        continue;
      }
      eligible.add(student);
    }
    return eligible;
  }

  private double satisfactionScore(int totalCost, int assignmentCount, int bookCount) {
    if (assignmentCount == 0 || bookCount <= 1) {
      return assignmentCount == 0 ? 0.0 : 1.0;
    }

    int maxPossibleCost = assignmentCount * (bookCount - 1);
    return 1.0 - ((double) totalCost / maxPossibleCost);
  }

  private AssignmentResult.SatisfactionDistribution satisfactionDistribution(
      Map<ClassState.Student, Integer> assignedRanks
  ) {
    int firstChoice = 0;
    int topThree = 0;
    int worseThanThird = 0;

    for (int rank : assignedRanks.values()) {
      if (rank == 1) {
        firstChoice++;
      }
      if (rank <= 3) {
        topThree++;
      } else {
        worseThanThird++;
      }
    }

    return new AssignmentResult.SatisfactionDistribution(firstChoice, topThree, worseThanThird);
  }

  private record StudentBookEdge(
      ClassState.Student student,
      ClassState.Book book,
      int rank,
      MinCostFlowGraph.Edge edge
  ) {
  }

  private static final class MinCostFlowGraph {
    private final List<List<Edge>> adjacency;

    MinCostFlowGraph(int nodeCount) {
      this.adjacency = new ArrayList<>(nodeCount);
      for (int i = 0; i < nodeCount; i++) {
        adjacency.add(new ArrayList<>());
      }
    }

    Edge addEdge(int from, int to, int capacity, int cost) {
      Edge forward = new Edge(to, adjacency.get(to).size(), capacity, cost);
      Edge reverse = new Edge(from, adjacency.get(from).size(), 0, -cost);
      adjacency.get(from).add(forward);
      adjacency.get(to).add(reverse);
      return forward;
    }

    FlowResult minCostMaxFlow(int source, int sink, int maxFlow) {
      int nodeCount = adjacency.size();
      int[] potentials = new int[nodeCount];
      int flow = 0;
      int cost = 0;

      while (flow < maxFlow) {
        int[] distances = new int[nodeCount];
        int[] previousNode = new int[nodeCount];
        int[] previousEdge = new int[nodeCount];
        boolean[] visited = new boolean[nodeCount];

        for (int i = 0; i < nodeCount; i++) {
          distances[i] = Integer.MAX_VALUE;
          previousNode[i] = -1;
          previousEdge[i] = -1;
        }
        distances[source] = 0;

        PriorityQueue<NodeDistance> queue = new PriorityQueue<>(
            Comparator.comparingInt(NodeDistance::distance).thenComparingInt(NodeDistance::node)
        );
        queue.add(new NodeDistance(source, 0));

        while (!queue.isEmpty()) {
          NodeDistance current = queue.poll();
          int node = current.node();
          if (visited[node]) {
            continue;
          }
          visited[node] = true;

          for (int edgeIndex = 0; edgeIndex < adjacency.get(node).size(); edgeIndex++) {
            Edge edge = adjacency.get(node).get(edgeIndex);
            if (edge.remainingCapacity() <= 0) {
              continue;
            }

            int next = edge.to();
            int reducedCost = edge.cost() + potentials[node] - potentials[next];
            int nextDistance = distances[node] + reducedCost;
            if (nextDistance < distances[next]) {
              distances[next] = nextDistance;
              previousNode[next] = node;
              previousEdge[next] = edgeIndex;
              queue.add(new NodeDistance(next, nextDistance));
            }
          }
        }

        if (previousNode[sink] == -1) {
          break;
        }

        for (int node = 0; node < nodeCount; node++) {
          if (distances[node] < Integer.MAX_VALUE) {
            potentials[node] += distances[node];
          }
        }

        int additionalFlow = maxFlow - flow;
        for (int node = sink; node != source; node = previousNode[node]) {
          Edge edge = adjacency.get(previousNode[node]).get(previousEdge[node]);
          additionalFlow = Math.min(additionalFlow, edge.remainingCapacity());
        }

        for (int node = sink; node != source; node = previousNode[node]) {
          Edge edge = adjacency.get(previousNode[node]).get(previousEdge[node]);
          edge.addFlow(additionalFlow);
          adjacency.get(edge.to()).get(edge.reverseIndex()).addFlow(-additionalFlow);
          cost += additionalFlow * edge.cost();
        }

        flow += additionalFlow;
      }

      return new FlowResult(flow, cost);
    }

    private record NodeDistance(int node, int distance) {
    }

    record FlowResult(int flow, int cost) {
    }

    static final class Edge {
      private final int to;
      private final int reverseIndex;
      private final int capacity;
      private final int cost;
      private int flow;

      Edge(int to, int reverseIndex, int capacity, int cost) {
        this.to = to;
        this.reverseIndex = reverseIndex;
        this.capacity = capacity;
        this.cost = cost;
      }

      int to() {
        return to;
      }

      int reverseIndex() {
        return reverseIndex;
      }

      int cost() {
        return cost;
      }

      int flow() {
        return flow;
      }

      int remainingCapacity() {
        return capacity - flow;
      }

      void addFlow(int amount) {
        flow += amount;
      }
    }
  }
}
