package com.transporte.bogota.util;

import com.transporte.bogota.model.Estacion;
import java.util.*;

/**
 * Grafo dirigido ponderado para representar la red de transporte.
 * Cada nodo es una estación y cada arista es una conexión con tiempo y capacidad.
 */
public class Graph {

    private final Map<Estacion, List<GraphEdge>> adj = new HashMap<>();

    public void addNodo(Estacion e) {
        adj.putIfAbsent(e, new ArrayList<>());
    }

    public void addArista(Estacion origen, Estacion destino, int tiempo, int capacidad) {
        addNodo(origen);
        addNodo(destino);
        adj.get(origen).add(new GraphEdge(origen, destino, tiempo, capacidad));
    }

    public List<GraphEdge> getVecinos(Estacion e) {
        return adj.getOrDefault(e, Collections.emptyList());
    }

    public Set<Estacion> getNodos() {
        return adj.keySet();
    }

    public boolean contiene(Estacion e) {
        return adj.containsKey(e);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Graph:\n");
        for (Estacion e : adj.keySet()) {
            sb.append(e.getNombre()).append(" -> ");
            for (GraphEdge edge : adj.get(e)) {
                sb.append(edge.getDestino().getNombre())
                  .append("(").append(edge.getTiempo()).append("min), ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
