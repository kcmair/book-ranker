package com.bookranker.algorithm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

final class MinCostFlowGraph {

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

      PriorityQueue<NodeDistance> queue =
          new PriorityQueue<>(
              Comparator.comparingInt(NodeDistance::distance).thenComparingInt(NodeDistance::node));
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

  private record NodeDistance(int node, int distance) {}

  record FlowResult(int flow, int cost) {}

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
