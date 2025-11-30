package com.transporte.bogota.algorithm;

import com.transporte.bogota.model.Estacion;
import com.transporte.bogota.util.Graph;
import com.transporte.bogota.util.GraphEdge;

import java.util.*;

/**
 * Implementación del algoritmo de Dijkstra para encontrar el camino más corto.
 * Utiliza tiempo de viaje como peso de las aristas.
 */
public class Dijkstra {

    public static class ResultadoDijkstra {
        public final int distancia;
        public final List<Estacion> camino;

        public ResultadoDijkstra(int distancia, List<Estacion> camino) {
            this.distancia = distancia;
            this.camino = camino;
        }
    }

    /**
     * Calcula el camino mínimo entre origen y destino.
     */
    public static ResultadoDijkstra calcularCaminoMinimo(Graph grafo, Estacion origen, Estacion destino) {
        Map<Estacion, Integer> dist = new HashMap<>();
        Map<Estacion, Estacion> prev = new HashMap<>();

        for (Estacion e : grafo.getNodos()) {
            dist.put(e, Integer.MAX_VALUE);
            prev.put(e, null);
        }

        dist.put(origen, 0);

        PriorityQueue<Estacion> pq = new PriorityQueue<>(Comparator.comparingInt(dist::get));
        pq.add(origen);

        while (!pq.isEmpty()) {
            Estacion actual = pq.poll();

            if (actual.equals(destino)) {
                break;
            }

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

        // Reconstruir camino
        List<Estacion> camino = new ArrayList<>();
        Estacion actual = destino;
        while (actual != null) {
            camino.add(actual);
            actual = prev.get(actual);
        }
        Collections.reverse(camino);

        int distanciaFinal = dist.getOrDefault(destino, Integer.MAX_VALUE);

        return new ResultadoDijkstra(distanciaFinal, camino);
    }
}
