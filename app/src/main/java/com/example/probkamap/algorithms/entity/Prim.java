package com.example.probkamap.algorithms.entity;

import android.util.Log;
import android.util.Pair;

import org.osmdroid.util.GeoPoint;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Prim
{
    public List<Vertex> graph;
    public Map<Dijkstra.Node, Vertex> nodeVertex = new HashMap<>();
    public Map<Vertex, Dijkstra.Node> vertexNode = new HashMap<>();

    public Prim(List<Vertex> graph)
    {
        this.graph = graph;
    }

    public Dijkstra.Graph toDijkstra()
    {
        Dijkstra.Graph dijkstraGraph = new Dijkstra.Graph();
        for (Vertex vertex: graph)
        {
            Dijkstra.Node node = new Dijkstra.Node(vertex.label);
            dijkstraGraph.addNode(node);
            nodeVertex.put(node, vertex);
            vertexNode.put(vertex, node);
        }
        for (Dijkstra.Node node: dijkstraGraph.getNodes())
        {
            for (Map.Entry<Vertex, Edge> entry: nodeVertex.get(node).edges.entrySet())
            {
                Edge edge = entry.getValue();
                if(edge.isIncluded)
                {
                    node.addDestination(vertexNode.get(entry.getKey()), edge.weight);
                }
                else
                {
                    Log.d("PRIM", "NOT INCLUDED");
                }
            }
        }
        return dijkstraGraph;
    }

    public void run() {
        if (graph.size() > 0) {
            graph.get(0).setVisited(true);
        }
        while (isDisconnected()) {
            Edge nextMinimum = new Edge(Double.MAX_VALUE);
            Vertex nextVertex = graph.get(0);
            for (Vertex vertex : graph) {
                if (vertex.isVisited()) {
                    Pair<Vertex, Edge> candidate = vertex.nextMinimum();
                    if (candidate.second.getWeight() < nextMinimum.getWeight()) {
                        nextMinimum = candidate.second;
                        nextVertex = candidate.first;
                    }
                }
            }
            nextMinimum.setIncluded(true);
            nextVertex.setVisited(true);
        }
    }

    private boolean isDisconnected() {
        for (Vertex vertex : graph) {
            if (!vertex.isVisited()) {
                return true;
            }
        }
        return false;
    }

    public class Edge
    {
        private double weight;
        private boolean isIncluded = false;

        public Edge(double weight)
        {
            this.weight = weight;
        }

        public double getWeight() {
            return weight;
        }

        public boolean isIncluded() {
            return isIncluded;
        }

        public void setIncluded(boolean included) {
            isIncluded = included;
        }
    }

    public class Vertex
    {
        private GeoPoint label;
        private Map<Vertex, Edge> edges = new HashMap<>();
        private boolean isVisited = false;

        public Vertex(GeoPoint label) {
            this.label = label;
        }

        public boolean isVisited() {
            return isVisited;
        }

        public void setVisited(boolean visited) {
            isVisited = visited;
        }

        public GeoPoint getLabel() {
            return label;
        }

        public void addEdge(Vertex vertex, Edge edge){
            if (this.edges.containsKey(vertex)){
                if (edge.getWeight() < this.edges.get(vertex).getWeight()){
                    this.edges.replace(vertex, edge);
                }
            } else {
                this.edges.put(vertex, edge);
            }
        }

        public Pair<Vertex, Edge> nextMinimum() {
            Edge nextMinimum = new Edge(Integer.MAX_VALUE);
            Vertex nextVertex = this;
            Iterator<Map.Entry<Vertex,Edge>> it = edges.entrySet()
                    .iterator();
            while (it.hasNext()) {
                Map.Entry<Vertex,Edge> pair = it.next();
                if (!pair.getKey().isVisited()) {
                    if (!pair.getValue().isIncluded()) {
                        if (pair.getValue().getWeight() < nextMinimum.getWeight()) {
                            nextMinimum = pair.getValue();
                            nextVertex = pair.getKey();
                        }
                    }
                }
            }
            return new Pair<>(nextVertex, nextMinimum);
        }

    }
}
