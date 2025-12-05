package com.transporte.bogota.algorithm;

import com.transporte.bogota.model.Estacion;
import com.transporte.bogota.util.Graph;
import com.transporte.bogota.util.GraphEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Implementación eficiente y correcta del algoritmo de Dijkstra para encontrar
 * el camino más corto en términos de tiempo entre dos estaciones.
 */
public class Dijkstra {

    private static final Logger logger = LoggerFactory.getLogger(Dijkstra.class);
    private static final double INFINITO = Double.POSITIVE_INFINITY;

    public static class ResultadoDijkstra {
        public final double distancia;
        public final List<Estacion> camino;

        public ResultadoDijkstra(double distancia, List<Estacion> camino) {
            this.distancia = distancia;
            this.camino = Collections.unmodifiableList(camino); // Inmutable por seguridad
        }

        public boolean esAlcanzable() {
            return distancia < INFINITO;
        }
    }

    /**
     * Calcula el camino más corto (mínimo tiempo) desde origen hasta destino.
     *
     * @param grafo   Grafo del sistema de transporte
     * @param origen  Estación de partida
     * @param destino Estación de llegada
     * @return Resultado con distancia y camino óptimo
     */
    public static ResultadoDijkstra calcularCaminoMinimo(Graph grafo, Estacion origen, Estacion destino) {
        if (origen == null || destino == null || grafo == null) {
            throw new IllegalArgumentException("Grafo, origen y destino no pueden ser null");
        }

        long startTime = System.currentTimeMillis();
        logger.info("Iniciando Dijkstra desde {} hacia {} ({} nodos)", origen, destino, grafo.getNodos().size());

        Map<Estacion, Double> distancias = new HashMap<>();
        Map<Estacion, Estacion> predecesores = new HashMap<>();
        Set<Estacion> visitados = new HashSet<>();

        // Cola de prioridad: menor distancia primero
        PriorityQueue<Estacion> cola = new PriorityQueue<>(
                Comparator.comparingDouble(n -> distancias.getOrDefault(n, INFINITO))
        );

        // Inicialización
        for (Estacion nodo : grafo.getNodos()) {
            distancias.put(nodo, INFINITO);
        }
        distancias.put(origen, 0.0);
        cola.offer(origen);

        while (!cola.isEmpty()) {
            Estacion actual = cola.poll();

            // Si ya fue procesado con una mejor o igual distancia, ignorar esta entrada duplicada
            if (visitados.contains(actual)) {
                continue;
            }

            visitados.add(actual);

            // Si llegamos al destino, podemos terminar (opcional: mejora rendimiento)
            if (actual.equals(destino)) {
                List<Estacion> camino = reconstruirCamino(predecesores, origen, destino);
                long tiempoEjecucion = System.currentTimeMillis() - startTime;
                logger.info("Dijkstra completado en {} ms | Distancia: {:.2f} min | {} estaciones",
                        tiempoEjecucion, distancias.get(destino) / 60.0, camino.size());
                return new ResultadoDijkstra(distancias.get(destino), camino);
            }

            // Relajación de aristas
            for (GraphEdge arista : grafo.getVecinos(actual)) {
                Estacion vecino = arista.getDestino();
                if (visitados.contains(vecino)) {
                    continue; // Ya procesado con mejor o igual distancia
                }

                double peso = arista.getTiempo();
                double distanciaCandidata = distancias.get(actual) + peso;

                if (distanciaCandidata < distancias.get(vecino)) {
                    distancias.put(vecino, distanciaCandidata);
                    predecesores.put(vecino, actual);
                    cola.offer(vecino); // Se permiten duplicados, se filtran con visitados
                }
            }
        }

        // No se encontró camino
        long tiempoEjecucion = System.currentTimeMillis() - startTime;
        logger.warn("No existe camino de {} a {} | Tiempo búsqueda: {} ms", origen, destino, tiempoEjecucion);
        return new ResultadoDijkstra(INFINITO, Collections.emptyList());
    }

    /**
     * Reconstruye el camino óptimo usando el mapa de predecesores.
     */
    private static List<Estacion> reconstruirCamino(Map<Estacion, Estacion> predecesores,
                                                    Estacion origen, Estacion destino) {
        List<Estacion> camino = new ArrayList<>();
        Estacion actual = destino;

        while (actual != null) {
            camino.add(actual);
            if (actual.equals(origen)) {
                Collections.reverse(camino);
                return camino;
            }
            actual = predecesores.get(actual);
        }

        // Si no se llegó al origen → no hay camino válido
        return Collections.emptyList();
    }
}