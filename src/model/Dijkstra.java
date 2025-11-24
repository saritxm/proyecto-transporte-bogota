package model;

import java.util.*;

public class Dijkstra {

    public static class ResultadoDijkstra {
        public final Map<Estacion, Integer> distancia;
        public final Map<Estacion, Estacion> anterior; // para reconstruir camino

        public ResultadoDijkstra(Map<Estacion, Integer> distancia,
                                 Map<Estacion, Estacion> anterior) {
            this.distancia = distancia;
            this.anterior = anterior;
        }

        // ---- Reconstruir camino ----
        public List<Estacion> getCamino(Estacion destino) {
            List<Estacion> camino = new ArrayList<>();
            Estacion actual = destino;
            while (actual != null) {
                camino.add(actual);
                actual = anterior.get(actual);
            }
            Collections.reverse(camino);
            return camino;
        }
    }

    /**
     * Ejecuta Dijkstra en tu grafo (Graph) usando el tiempo de viaje como peso.
     */
    public static ResultadoDijkstra calcular(Graph grafo, Estacion origen) {
        // Distancia inicial infinita
        Map<Estacion, Integer> dist = new HashMap<>();
        Map<Estacion, Estacion> prev = new HashMap<>();

        for (Estacion e : grafo.getNodos()) {
            dist.put(e, Integer.MAX_VALUE);
            prev.put(e, null);
        }

        dist.put(origen, 0);

        // Cola de prioridad por distancia mínima
        PriorityQueue<Estacion> pq = new PriorityQueue<>(
                Comparator.comparingInt(dist::get)
        );
        pq.add(origen);

        while (!pq.isEmpty()) {
            Estacion actual = pq.poll();

            for (GraphEdge edge : grafo.getVecinos(actual)) {

                Estacion vecino = edge.getDestino();
                int nuevoCosto = dist.get(actual) + edge.getTiempo();

                if (nuevoCosto < dist.get(vecino)) {
                    dist.put(vecino, nuevoCosto);
                    prev.put(vecino, actual);

                    pq.remove(vecino);
                    pq.add(vecino);
                }
            }
        }

        return new ResultadoDijkstra(dist, prev);
    }
}
