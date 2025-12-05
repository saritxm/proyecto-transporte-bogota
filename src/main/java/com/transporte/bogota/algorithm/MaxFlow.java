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
        // Validar que origen y destino existan en el grafo
        if (!grafo.contiene(origen) || !grafo.contiene(destino)) {
            return 0;
        }
        
        int flujoMaximo = 0;
        
        // 1. Inicializar la capacidad residual basada en el grafo original
        inicializarCapacidadResidual(grafo);
        
        // Mapa para almacenar el camino de aumento encontrado por BFS (parent[v] = u)
        Map<Estacion, Estacion> parent = new HashMap<>();
        
        // Límite de iteraciones para evitar bucles infinitos en grafos degenerados
        int maxIteraciones = Math.min(10000, grafo.getNodos().size() * 2);
        int iteraciones = 0;

        // 2. Ejecutar el ciclo principal de Ford-Fulkerson
        // Mientras haya un camino de aumento desde origen a destino en el grafo residual
        while (iteraciones < maxIteraciones && encontrarCaminoAumento(origen, destino, parent, grafo.getNodos())) {
            iteraciones++;
            
            // 3. Encontrar el flujo (cuello de botella) del camino encontrado
            int flujoCamino = Integer.MAX_VALUE;
            Estacion v = destino;
            while (!v.equals(origen)) {
                Estacion u = parent.get(v);
                Map<Estacion, Integer> capacidades = capacidadResidual.get(u);
                int capActual = capacidades != null ? capacidades.getOrDefault(v, 0) : 0;
                flujoCamino = Math.min(flujoCamino, capActual);
                v = u;
            }
            
            // Si flujoCamino es 0 o MAX_VALUE, hay un problema, salir
            if (flujoCamino <= 0 || flujoCamino == Integer.MAX_VALUE) {
                break;
            }
            
            // 4. Sumar el flujo del camino al flujo máximo total
            flujoMaximo += flujoCamino;

            // 5. Actualizar las capacidades residuales (grafo residual)
            v = destino;
            while (!v.equals(origen)) {
                Estacion u = parent.get(v);
                
                // Reducir la capacidad en la arista de avance
                Map<Estacion, Integer> capacidadesU = capacidadResidual.get(u);
                capacidadesU.put(v, capacidadesU.get(v) - flujoCamino);
                
                // Aumentar la capacidad en la arista de retroceso
                Map<Estacion, Integer> capacidadesV = capacidadResidual.get(v);
                capacidadesV.put(u, capacidadesV.getOrDefault(u, 0) + flujoCamino);
                
                v = u;
            }
        }

        return flujoMaximo;
    }
    
    // ================== Métodos Auxiliares ==================

    /**
     * Inicializa la capacidad residual usando matriz dispersa.
     * Solo crea entradas para aristas existentes, no para todos los pares nodo×nodo.
     * Esto reduce memoria de O(n²) a O(m) donde m es el número de aristas.
     */
    private static void inicializarCapacidadResidual(Graph grafo) {
        capacidadResidual = new HashMap<>();
        
        // 1. Crear entrada para cada nodo (pero sin inicializar todos los pares)
        for (Estacion u : grafo.getNodos()) {
            capacidadResidual.put(u, new HashMap<>());
        }
        
        // 2. Llenar solo con las aristas reales del grafo
        for (Estacion u : grafo.getNodos()) {
            Map<Estacion, Integer> vecinos = capacidadResidual.get(u);
            for (GraphEdge edge : grafo.getVecinos(u)) {
                Estacion destino = edge.getDestino();
                // Capacidad de avance
                vecinos.put(destino, edge.getCapacidad());
                
                // Asegurar que el nodo destino existe en el mapa (aristas de retroceso)
                capacidadResidual.putIfAbsent(destino, new HashMap<>());
                // Capacidad de retroceso inicial es 0
                capacidadResidual.get(destino).putIfAbsent(u, 0);
            }
        }
    }

    /**
     * Búsqueda en anchura (BFS) para encontrar un camino de aumento.
     * OPTIMIZADO: Solo itera sobre vecinos reales (aristas existentes), no todos los nodos.
     */
    private static boolean encontrarCaminoAumento(Estacion s, Estacion t, Map<Estacion, Estacion> parent, Collection<Estacion> nodos) {
        parent.clear(); 
        Set<Estacion> visitados = new HashSet<>();
        Queue<Estacion> queue = new LinkedList<>();

        queue.add(s);
        visitados.add(s);
        
        while (!queue.isEmpty()) {
            Estacion u = queue.poll();
            
            // Iterar solo sobre vecinos reales (con capacidad residual > 0)
            Map<Estacion, Integer> vecinosResidual = capacidadResidual.get(u);
            if (vecinosResidual != null) {
                for (Map.Entry<Estacion, Integer> entry : vecinosResidual.entrySet()) {
                    Estacion v = entry.getKey();
                    int capacidad = entry.getValue();
                    
                    // Si aún queda capacidad residual y no ha sido visitado
                    if (capacidad > 0 && !visitados.contains(v)) {
                        queue.add(v);
                        parent.put(v, u);
                        visitados.add(v);
                        
                        if (v.equals(t)) {
                            return true; // Camino encontrado
                        }
                    }
                }
            }
        }
        
        return false; // No hay más caminos de aumento
    }
}