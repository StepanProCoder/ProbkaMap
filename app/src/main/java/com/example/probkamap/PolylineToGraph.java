package com.example.probkamap;

import org.osmdroid.util.GeoPoint;
import java.util.*;

class Graph {
    private final Map<GeoPoint, List<GeoPoint>> adjacencyList;

    public Graph() {
        this.adjacencyList = new HashMap<>();
    }

    public void addEdge(GeoPoint source, GeoPoint destination) {
        adjacencyList.putIfAbsent(source, new ArrayList<>());
        adjacencyList.putIfAbsent(destination, new ArrayList<>());
        adjacencyList.get(source).add(destination);
        adjacencyList.get(destination).add(source);
    }

    public List<GeoPoint> getNeighbors(GeoPoint point) {
        return adjacencyList.getOrDefault(point, new ArrayList<>());
    }
}

public class PolylineToGraph {
    public static void main(String[] args) {
        // Пример полилинии
        List<GeoPoint> polyline = Arrays.asList(
                new GeoPoint(55.7558, 37.6176),
                new GeoPoint(55.7559, 37.6177),
                new GeoPoint(55.7560, 37.6178),
                new GeoPoint(55.7561, 37.6179),
                new GeoPoint(55.7562, 37.6180)
        );

        // Создаем граф
        Graph graph = new Graph();
        for (int i = 0; i < polyline.size() - 1; i++) {
            graph.addEdge(polyline.get(i), polyline.get(i + 1));
        }

        // Определяем начальную и конечную точки
        GeoPoint source = polyline.get(0);
        GeoPoint destination = polyline.get(polyline.size() - 1);

        // Вычисляем кратчайший путь с помощью алгоритма Дейкстры
        Map<GeoPoint, GeoPoint> predecessors = calculateShortestPath(graph, source);

        // Восстанавливаем путь от начальной до конечной точки
        List<GeoPoint> shortestPath = reconstructPath(predecessors, destination);

        // Выводим отфильтрованный полилайн
        System.out.println("Отфильтрованный полилайн (кратчайший путь):");
        for (GeoPoint point : shortestPath) {
            System.out.println(point);
        }
    }

    public static Map<GeoPoint, GeoPoint> calculateShortestPath(Graph graph, GeoPoint source) {
        PriorityQueue<Map.Entry<GeoPoint, Double>> priorityQueue = new PriorityQueue<>(Map.Entry.comparingByValue());
        Map<GeoPoint, Double> distances = new HashMap<>();
        Map<GeoPoint, GeoPoint> predecessors = new HashMap<>();
        Set<GeoPoint> visited = new HashSet<>();

        distances.put(source, 0.0);
        priorityQueue.add(new AbstractMap.SimpleEntry<>(source, 0.0));

        while (!priorityQueue.isEmpty()) {
            Map.Entry<GeoPoint, Double> currentEntry = priorityQueue.poll();
            GeoPoint currentPoint = currentEntry.getKey();

            if (!visited.contains(currentPoint)) {
                visited.add(currentPoint);
                double currentDistance = currentEntry.getValue();

                for (GeoPoint neighbor : graph.getNeighbors(currentPoint)) {
                    double distance = currentDistance + 1;  // Используем вес ребра как 1
                    if (distance < distances.getOrDefault(neighbor, Double.MAX_VALUE)) {
                        distances.put(neighbor, distance);
                        predecessors.put(neighbor, currentPoint);
                        priorityQueue.add(new AbstractMap.SimpleEntry<>(neighbor, distance));
                    }
                }
            }
        }

        return predecessors;
    }

    public static List<GeoPoint> reconstructPath(Map<GeoPoint, GeoPoint> predecessors, GeoPoint target) {
        List<GeoPoint> path = new ArrayList<>();
        for (GeoPoint at = target; at != null; at = predecessors.get(at)) {
            path.add(at);
        }
        Collections.reverse(path);
        return path;
    }

}
