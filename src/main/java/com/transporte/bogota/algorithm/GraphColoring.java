package com.transporte.bogota.algorithm;

import com.transporte.bogota.model.Estacion;
import com.transporte.bogota.util.Graph;
import com.transporte.bogota.util.GraphEdge;

import java.util.*;

/**
 * Implementación de una heurística de coloreado de grafos (Welsh-Powell)
 * para optimizar la asignación de recursos (e.g., franjas horarias, andenes)
 * sin conflicto.
 * * Los Nodos son los recursos/servicios, y una arista significa un conflicto.
 */
public class GraphColoring {

    /**
     * Colorea el grafo de conflicto usando la heurística de Welsh-Powell.
     * @param conflictoGraph El grafo donde las aristas representan conflictos.
     * @return Un mapa donde la clave es el nodo (recurso) y el valor es el ID del color (franja horaria/andén).
     */
    public static Map<Estacion, Integer> colorearGrafo(Graph conflictoGraph) {
        Map<Estacion, Integer> colores = new HashMap<>();
        
        // 1. Obtener los nodos del grafo
        List<Estacion> nodos = new ArrayList<>(conflictoGraph.getNodos());

        // 2. Ordenar los nodos por grado decreciente (Heurística Welsh-Powell)
        nodos.sort(Comparator.comparingInt((Estacion n) -> conflictoGraph.getVecinos(n).size()).reversed());
        
        int colorActual = 1;
        
        // 3. Colorear los nodos
        for (Estacion nodoInicial : nodos) {
            if (!colores.containsKey(nodoInicial)) {
                // Asignar un nuevo color al nodo inicial no coloreado
                colores.put(nodoInicial, colorActual);
                
                // Iterar sobre los nodos restantes para asignar el mismo color
                for (Estacion otroNodo : nodos) {
                    if (!colores.containsKey(otroNodo)) {
                        boolean conflicto = false;
                        
                        // Verificar si el 'otroNodo' tiene conflicto con cualquier vecino ya coloreado con 'colorActual'
                        for (GraphEdge vecinoEdge : conflictoGraph.getVecinos(otroNodo)) {
                            Estacion vecino = vecinoEdge.getDestino();
                            if (colores.containsKey(vecino) && colores.get(vecino) == colorActual) {
                                conflicto = true;
                                break;
                            }
                        }
                        
                        if (!conflicto) {
                            colores.put(otroNodo, colorActual);
                        }
                    }
                }
                
                // Mover al siguiente color
                colorActual++;
            }
        }
        
        return colores;
    }
}