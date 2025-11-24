package model;
import java.util.*;

public class Graph {

    // Grafo dirigido: una estación -> lista de aristas hacia otra estación
    private final Map<Estacion, List<GraphEdge>> adj = new HashMap<>();

    // ---- Métodos básicos ----

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

    public boolean contiene(Estacion e) { return adj.containsKey(e); }

    // ---- Utilidades ----

    public Graph obtenerGrafoSinCapacidades() {
        Graph g = new Graph();
        for (Estacion e : adj.keySet()) {
            for (GraphEdge edge : adj.get(e)) {
                g.addArista(edge.getOrigen(), edge.getDestino(), edge.getTiempo(), 0);
            }
        }
        return g;
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
