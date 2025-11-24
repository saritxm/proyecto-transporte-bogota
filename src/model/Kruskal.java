package model;

import java.util.*;

/**
 * Kruskal para tu grafo. Como tu Graph es dirigido, tratamos aristas como no dirigidas
 * para calcular un MST (evitamos duplicados por par origen-destino).
 * Usamos peso = tiempo (GraphEdge.getTiempo()).
 */
public class Kruskal {

    private static class UF {
        private final Map<Estacion, Estacion> parent = new HashMap<>();
        public void makeSet(Collection<Estacion> nodes) {
            for (Estacion e : nodes) parent.put(e, e);
        }
        public Estacion find(Estacion a) {
            Estacion p = parent.get(a);
            if (p == a) return a;
            Estacion r = find(p);
            parent.put(a, r);
            return r;
        }
        public void union(Estacion a, Estacion b) {
            Estacion ra = find(a), rb = find(b);
            if (ra != rb) parent.put(ra, rb);
        }
    }

    public static List<GraphEdge> mst(Graph g) {
        // Obtener lista única de aristas no dirigidas (usar par ordenado)
        Set<String> seenPairs = new HashSet<>();
        List<GraphEdge> edges = new ArrayList<>();

        for (Estacion u : g.getNodos()) {
            for (GraphEdge e : g.getVecinos(u)) {
                String a = e.getOrigen().getId();
                String b = e.getDestino().getId();
                String key = a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
                if (!seenPairs.contains(key)) {
                    seenPairs.add(key);
                    edges.add(e);
                }
            }
        }

        // ordenar por peso (tiempo)
        edges.sort(Comparator.comparingInt(GraphEdge::getTiempo));

        // union-find
        UF uf = new UF();
        uf.makeSet(g.getNodos());

        List<GraphEdge> mst = new ArrayList<>();
        for (GraphEdge e : edges) {
            Estacion u = e.getOrigen();
            Estacion v = e.getDestino();
            if (uf.find(u) != uf.find(v)) {
                mst.add(e);
                uf.union(u, v);
            }
        }

        return mst;
    }
}
