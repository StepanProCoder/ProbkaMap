package com.example.probkamap.algorithms.entity;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DFS {
    private final List<Prim.Vertex> graph;
    private final Set<Prim.Vertex> visited = new HashSet<>();

    public DFS(List<Prim.Vertex> graph) {
        this.graph = graph;
    }

    public List<Prim.Vertex> getGraph() {
        return graph;
    }

    // Метод для проверки достижимости целевой вершины из начальной без ребра между ними
    public boolean isReachableWithoutDirectEdge(Prim.Vertex startVertex, Prim.Vertex targetVertex) {
        visited.clear(); // Очищаем множество посещённых вершин перед каждым запуском
        return dfs(startVertex, targetVertex);
    }

    private boolean dfs(Prim.Vertex vertex, Prim.Vertex targetVertex) {
        // Если вершина уже посещена, пропускаем её
        if (visited.contains(vertex)) {
            return false;
        }

        // Отмечаем текущую вершину как посещённую
        visited.add(vertex);

        // Если достигли целевой вершины, возвращаем false, потому что это игнорируемое ребро
        if (vertex.equals(targetVertex)) {
            return false;
        }

        // Рекурсивно обходим всех соседей текущей вершины
        for (Map.Entry<Prim.Vertex, Prim.Edge> entry : vertex.getEdges().entrySet()) {
            Prim.Vertex neighbor = entry.getKey();

            // Пропускаем ребро между `vertex` и `targetVertex`
            if (neighbor.equals(targetVertex) && vertex.getEdges().containsKey(targetVertex)) {
                continue;
            }

            // Проверяем достижимость остальных соседей
            if (dfs(neighbor, targetVertex)) {
                return true; // Если нашли путь к целевой вершине, возвращаем true
            }
        }

        return false; // Если целевая вершина не найдена
    }
}
