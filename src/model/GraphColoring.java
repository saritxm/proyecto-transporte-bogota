package model;

import java.util.*;

/**
 * Coloreado greedy adaptado a tu Graph. Considera vecinos salientes y entrantes.
 * Retorna un Map<Estacion, Integer> con color (0..k).
 */
public class GraphColoring {

    // obtiene vecinos (entrantes + salientes) para cada estación
    private static Map<Estacion, Set<Estacion>> buildNeighborMap(Graph g) {
        Map<Estacion, Set<Estacion>> neigh = new HashMap<>();
        for (Estacion u : g.getNodos()) neigh.put(u, new HashSet<>());

        for (Estacion u : g.getNodos()) {
            for (GraphEdge e : g.getVecinos(u)) {
                Estacion v = e.getDestino();
                neigh.get(u).add(v);
                // añadir ingreso (v tiene u como vecino entrante)
                neigh.get(v).add(u);
            }
        }
        return neigh;
    }

    public static Map<Estacion, Integer> greedyColor(Graph g) {
        Map<Estacion, Integer> color = new HashMap<>();
        Map<Estacion, Set<Estacion>> neigh = buildNeighborMap(g);

        // ordenar estaciones por grado descendente (heurística)
        List<Estacion> nodes = new ArrayList<>(g.getNodos());
        nodes.sort((a, b) -> Integer.compare(neigh.get(b).size(), neigh.get(a).size()));

        for (Estacion u : nodes) {
            boolean[] used = new boolean[nodes.size() + 1];
            for (Estacion v : neigh.get(u)) {
                Integer c = color.get(v);
                if (c != null && c < used.length) used[c] = true;
            }
            int c = 0;
            while (c < used.length && used[c]) c++;
            color.put(u, c);
        }

        return color;
    }
}
