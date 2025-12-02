package com.transporte.bogota.algorithm;

import com.transporte.bogota.model.Estacion;
import com.transporte.bogota.util.Graph;
import com.transporte.bogota.util.GraphEdge;

import java.util.*;

/**
 * Implementación del algoritmo de Dijkstra para encontrar el camino más corto
 * entre dos estaciones en términos de tiempo (peso de la arista).
 * * Asume que las clases Estacion implementan correctamente equals() y hashCode().
 */
public class Dijkstra {

    public static class ResultadoDijkstra {
        public final double distancia;
        public final List<Estacion> camino;

        public ResultadoDijkstra(double distancia, List<Estacion> camino) {
            this.distancia = distancia;
            this.camino = camino;
        }
    }

    private static final double INFINITY = Double.POSITIVE_INFINITY;

    /**
     * Calcula el camino más corto (mínimo tiempo) desde una estación de origen a una de destino.
     * @param grafo El grafo del sistema de transporte.
     * @param origen La estación inicial.
     * @param destino La estación final.
     * @return ResultadoDijkstra con la distancia total y la lista de estaciones.
     */
    public static ResultadoDijkstra calcularCaminoMinimo(Graph grafo, Estacion origen, Estacion destino) {
        // Almacena la distancia más corta conocida desde el origen hasta cada estación.
        Map<Estacion, Double> distancias = new HashMap<>();
        // Almacena el predecesor de cada estación en la ruta más corta.
        Map<Estacion, Estacion> predecesores = new HashMap<>();
        
        // Cola de prioridad que almacena Estaciones, priorizando la de menor distancia.
        // Usamos un Comparator para mantener la propiedad de la cola de prioridad.
        PriorityQueue<Estacion> colaPrioridad = new PriorityQueue<>(
            Comparator.comparingDouble(e -> distancias.getOrDefault(e, INFINITY))
        );

        // Inicialización: todas las distancias son infinitas, excepto el origen (distancia 0).
        for (Estacion e : grafo.getNodos()) {
            distancias.put(e, INFINITY);
        }
        distancias.put(origen, 0.0);
        colaPrioridad.add(origen);

        while (!colaPrioridad.isEmpty()) {
            Estacion u = colaPrioridad.poll();
            
            // Si la distancia que acabamos de sacar de la cola es mayor a la ya registrada, 
            // significa que es un elemento "obsoleto" (duplicado) y lo ignoramos.
            if (distancias.get(u) > INFINITY && distancias.get(u) > distancias.getOrDefault(u, 0.0)) {
                continue;
            }

            if (u.equals(destino)) {
                // Si llegamos al destino, reconstruimos el camino y terminamos.
                return new ResultadoDijkstra(distancias.get(destino), reconstruirCamino(predecesores, origen, destino));
            }

            // Relajación de aristas
            for (GraphEdge edge : grafo.getVecinos(u)) {
                Estacion v = edge.getDestino();
                double peso = edge.getTiempo(); // El peso es el tiempo de viaje

                double nuevaDistancia = distancias.get(u) + peso;

                if (nuevaDistancia < distancias.get(v)) {
                    // Actualizar distancia y predecesor
                    distancias.put(v, nuevaDistancia);
                    predecesores.put(v, u);
                    
                    // Al no poder hacer un "decrease-key" eficiente, la técnica común es 
                    // simplemente agregar la Estacion a la cola de prioridad. La verificación 
                    // de nodos obsoletos (línea 55) manejará los duplicados.
                    colaPrioridad.add(v);
                }
            }
        }
        
        // Si el destino no es alcanzable
        return new ResultadoDijkstra(INFINITY, Collections.emptyList());
    }

    /**
     * Reconstruye el camino desde el origen hasta el destino usando el mapa de predecesores.
     */
    private static List<Estacion> reconstruirCamino(Map<Estacion, Estacion> predecesores, Estacion origen, Estacion destino) {
        LinkedList<Estacion> camino = new LinkedList<>();
        Estacion paso = destino;
        
        // Recorre hacia atrás desde el destino hasta encontrar el origen o un nodo sin predecesor
        while (paso != null) {
            camino.addFirst(paso);
            if (paso.equals(origen)) break;
            paso = predecesores.get(paso);
        }

        // Si el origen es el primer elemento, el camino es válido. Si no, está incompleto o no existe.
        if (!camino.isEmpty() && camino.getFirst().equals(origen)) {
            return camino;
        } else {
            return Collections.emptyList();
        }
    }
}