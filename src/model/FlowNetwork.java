package model;
import java.util.*;

public class FlowNetwork {

    private final int n;                 // número de nodos internos
    private final int[][] capacidad;     // matriz de capacidades
    private final List<Integer>[] adj;   // lista de adyacencia

    public FlowNetwork(int n) {
        this.n = n;
        capacidad = new int[n][n];
        adj = new List[n];
        for (int i = 0; i < n; i++) adj[i] = new ArrayList<>();
    }

    public void addEdge(int u, int v, int cap) {
        adj[u].add(v);
        adj[v].add(u); // necesario para grafo residual
        capacidad[u][v] += cap;
    }

    public int[][] getCapacidad() { return capacidad; }
    public List<Integer>[] getAdj() { return adj; }
    public int size() { return n; }
}
