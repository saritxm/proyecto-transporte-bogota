package com.transporte.bogota.algorithm;

import com.transporte.bogota.model.Estacion;
import com.transporte.bogota.util.Graph;
import com.transporte.bogota.util.GraphEdge;

import java.util.*;

/**
 * Estructura auxiliar para el algoritmo de Kruskal (Union-Find / DSU).
 * Se utiliza para detectar ciclos.
 */
class UnionFind {
    private final Map<Estacion, Estacion> parent;

    public UnionFind(Collection<Estacion> nodos) {
        parent = new HashMap<>();
        for (Estacion e : nodos) {
            parent.put(e, e); 
        }
    }

    /** Encuentra el representante (raíz) del conjunto al que pertenece el nodo. */
    public Estacion find(Estacion i) {
        if (parent.get(i).equals(i))
            return i;
        
        // Compresión de camino para optimización
        parent.put(i, find(parent.get(i)));
        return parent.get(i);
    }

    /** Une los conjuntos que contienen a 'x' y 'y'. */
    public boolean union(Estacion x, Estacion y) {
        Estacion rootX = find(x);
        Estacion rootY = find(y);

        if (!rootX.equals(rootY)) {
            parent.put(rootY, rootX);
            return true; // Se hizo la unión (no había ciclo)
        }
        return false; // Ya estaban unidos (había ciclo)
    }
}

/**
 * Implementación del algoritmo de Kruskal para encontrar el Árbol de Recubrimiento Mínimo (ARM).
 * Utiliza el tiempo de viaje como peso (costo) para la optimización de conexiones.
 */
public class MinimumSpanningTree {

    /**
     * Calcula el ARM usando Kruskal.
     * @param grafo El grafo de la red de transporte.
     * @return Una lista de GraphEdge que forman el ARM.
     */
    public static List<GraphEdge> calcularARM(Graph grafo) {
        List<GraphEdge> arm = new ArrayList<>();
        
        // 1. Obtener todas las aristas y ordenarlas por peso (tiempo de viaje)
        List<GraphEdge> todasLasAristas = obtenerTodasLasAristas(grafo);
        
        // Ordenar por el peso (tiempo de viaje) de forma ascendente
        todasLasAristas.sort(Comparator.comparingInt(GraphEdge::getTiempo));
        
        // 2. Inicializar la estructura Union-Find (DSU)
        UnionFind dsu = new UnionFind(grafo.getNodos());
        
        // 3. Iterar sobre las aristas ordenadas
        for (GraphEdge edge : todasLasAristas) {
            Estacion origen = edge.getOrigen();
            Estacion destino = edge.getDestino();

            // 4. Si la arista no crea un ciclo
            if (dsu.union(origen, destino)) {
                arm.add(edge);
                
                // 5. Condición de parada: N-1 aristas
                if (arm.size() == grafo.getNodos().size() - 1) {
                    break;
                }
            }
        }
        
        return arm;
    }
    
    /**
     * Método auxiliar para obtener todas las aristas del grafo.
     * Nota: Este método asume que el grafo subyacente es no dirigido
     * para el propósito del ARM, pero usa las aristas dirigidas existentes.
     */
    private static List<GraphEdge> obtenerTodasLasAristas(Graph grafo) {
        Set<GraphEdge> aristasSet = new HashSet<>();
        for (Estacion u : grafo.getNodos()) {
            aristasSet.addAll(grafo.getVecinos(u));
        }
        return new ArrayList<>(aristasSet);
    }
}