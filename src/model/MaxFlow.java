package model;

import java.util.*;

/**
 * MaxFlow usando Edmonds-Karp adaptado a tu Graph (Estacion, GraphEdge).
 * Construye una matriz de capacidades entre índices de estaciones y ejecuta Edmonds-Karp.
 */
public class MaxFlow {

    /**
     * Calcula flujo máximo entre source y sink en el grafo usando capacidades de GraphEdge.
     * @param g Grafo (model.Graph)
     * @param source Estacion origen
     * @param sink Estacion sumidero
     * @return flujo máximo (int)
     */
    public static int edmondsKarp(Graph g, Estacion source, Estacion sink) {
        // Mapear estaciones a índices 0..n-1
        List<Estacion> estaciones = new ArrayList<>(g.getNodos());
        Map<Estacion, Integer> id = new HashMap<>();
        for (int i = 0; i < estaciones.size(); i++) id.put(estaciones.get(i), i);

        int n = estaciones.size();
        int[][] capacity = new int[n][n];

        // Llenar matriz de capacidades con las capacidades de las aristas
        for (Estacion u : estaciones) {
            int ui = id.get(u);
            for (GraphEdge edge : g.getVecinos(u)) {
                Estacion v = edge.getDestino();
                Integer vi = id.get(v);
                if (vi != null) {
                    capacity[ui][vi] += edge.getCapacidad(); // sumar si varias aristas
                }
            }
        }

        int s = id.get(source);
        int t = id.get(sink);

        // matriz residual inicial
        int[][] residual = new int[n][n];
        for (int i = 0; i < n; i++) residual[i] = Arrays.copyOf(capacity[i], n);

        int maxFlow = 0;
        int[] parent = new int[n];

        while (bfs(residual, s, t, parent)) {
            // encontrar flujo mínimo en el camino encontrado
            int flow = Integer.MAX_VALUE;
            int v = t;
            while (v != s) {
                int u = parent[v];
                flow = Math.min(flow, residual[u][v]);
                v = u;
            }

            // actualizar residual
            v = t;
            while (v != s) {
                int u = parent[v];
                residual[u][v] -= flow;
                residual[v][u] += flow;
                v = u;
            }

            maxFlow += flow;
        }

        return maxFlow;
    }

    // BFS para encontrar camino aumentante; llena parent[]
    private static boolean bfs(int[][] residual, int s, int t, int[] parent) {
        Arrays.fill(parent, -1);
        Queue<Integer> q = new LinkedList<>();
        q.add(s);
        parent[s] = -2; // marca raíz

        while (!q.isEmpty()) {
            int u = q.poll();
            for (int v = 0; v < residual.length; v++) {
                if (parent[v] == -1 && residual[u][v] > 0) {
                    parent[v] = u;
                    if (v == t) return true;
                    q.add(v);
                }
            }
        }
        return false;
    }
}
