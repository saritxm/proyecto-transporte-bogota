package com.transporte.bogota.algorithm;

import com.transporte.bogota.model.Estacion;
import com.transporte.bogota.util.Graph;
import com.transporte.bogota.util.GraphEdge;

import java.util.*;

/**
 * Implementación del algoritmo de Edmonds-Karp para Flujo Máximo.
 * Utiliza la capacidad de la arista (capacidad del vehículo/tramo) 
 * para encontrar el flujo máximo de pasajeros entre origen y destino, 
 * lo que ayuda a identificar cuellos de botella (congestión).
 */
public class MaxFlow {

    /** * Matriz de capacidad residual: Almacena la capacidad restante disponible 
     * en la dirección de avance y retroceso entre estaciones.
     */
    private static Map<Estacion, Map<Estacion, Integer>> capacidadResidual;

    /**
     * Calcula el flujo máximo de pasajeros que puede pasar de origen a destino.
     * @param grafo La red de transporte modelada.
     * @param origen La estación fuente (source).
     * @param destino La estación sumidero (sink).
     * @return El flujo máximo total.
     */
    public static int calcularFlujoMaximo(Graph grafo, Estacion origen, Estacion destino) {
        int flujoMaximo = 0;
        
        // 1. Inicializar la capacidad residual basada en el grafo original
        inicializarCapacidadResidual(grafo);
        
        // Mapa para almacenar el camino de aumento encontrado por BFS (parent[v] = u)
        Map<Estacion, Estacion> parent = new HashMap<>();

        // 2. Ejecutar el ciclo principal de Ford-Fulkerson
        // Mientras haya un camino de aumento desde origen a destino en el grafo residual
        while (encontrarCaminoAumento(origen, destino, parent, grafo.getNodos())) {
            
            // 3. Encontrar el flujo (cuello de botella) del camino encontrado
            int flujoCamino = Integer.MAX_VALUE;
            Estacion v = destino;
            while (!v.equals(origen)) {
                Estacion u = parent.get(v);
                flujoCamino = Math.min(flujoCamino, capacidadResidual.get(u).get(v));
                v = u;
            }
            
            // 4. Sumar el flujo del camino al flujo máximo total
            flujoMaximo += flujoCamino;

            // 5. Actualizar las capacidades residuales (grafo residual)
            v = destino;
            while (!v.equals(origen)) {
                Estacion u = parent.get(v);
                
                // Reducir la capacidad en la arista de avance
                capacidadResidual.get(u).put(v, capacidadResidual.get(u).get(v) - flujoCamino);
                
                // Aumentar la capacidad en la arista de retroceso
                capacidadResidual.get(v).put(u, capacidadResidual.get(v).get(u) + flujoCamino);
                
                v = u;
            }
        }

        return flujoMaximo;
    }
    
    // ================== Métodos Auxiliares ==================

    /**
     * Inicializa la capacidad residual (matriz) a partir de las aristas del grafo.
     */
    private static void inicializarCapacidadResidual(Graph grafo) {
        capacidadResidual = new HashMap<>();
        
        // Inicializar todas las capacidades a 0 y asegurar que todas las estaciones estén en el mapa
        for (Estacion u : grafo.getNodos()) {
            capacidadResidual.put(u, new HashMap<>());
            for (Estacion v : grafo.getNodos()) {
                capacidadResidual.get(u).put(v, 0); 
            }
        }
        
        // Llenar con las capacidades reales (capacidades de la Ruta)
        for (Estacion u : grafo.getNodos()) {
            for (GraphEdge edge : grafo.getVecinos(u)) {
                // Asume que GraphEdge tiene getCapacidad()
                capacidadResidual.get(u).put(edge.getDestino(), edge.getCapacidad());
            }
        }
    }

    /**
     * Búsqueda en anchura (BFS) para encontrar un camino de aumento.
     */
    private static boolean encontrarCaminoAumento(Estacion s, Estacion t, Map<Estacion, Estacion> parent, Collection<Estacion> nodos) {
        parent.clear(); 
        Set<Estacion> visitados = new HashSet<>();
        Queue<Estacion> queue = new LinkedList<>();

        queue.add(s);
        visitados.add(s);
        
        while (!queue.isEmpty()) {
            Estacion u = queue.poll();
            
            // Recorrer todos los posibles vecinos (aristas reales y aristas de retroceso)
            for (Estacion v : nodos) { 
                // Si el nodo no ha sido visitado y aún queda capacidad residual
                if (!visitados.contains(v) && capacidadResidual.get(u).get(v) > 0) {
                    queue.add(v);
                    parent.put(v, u);
                    visitados.add(v);
                    
                    if (v.equals(t)) {
                        return true; // Camino encontrado
                    }
                }
            }
        }
        
        return false; // No hay más caminos de aumento
    }
}